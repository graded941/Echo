const fs = require('fs');
const path = require('path');
const {
    Document,
    Packer,
    Paragraph,
    TextRun,
    Table,
    TableRow,
    TableCell,
    HeadingLevel,
    AlignmentType,
    WidthType,
    BorderStyle,
    ShadingType,
    Header,
    Footer,
    PageNumber,
    PageBreak,
    TableOfContents
} = require('docx');

const REV_DIR = 'C:/Users/Administrator/Downloads/Apps/rev';
const DOCBUILD_DIR = path.join(REV_DIR, 'docbuild');
const OUTPUT_FILE = path.join(DOCBUILD_DIR, 'iPulse_5.9.1.17_ECHO_Rebuild_Spec.docx');

const PARTS = [
    'part-00-cover.md',
    'part-01-executive.md',
    'part-02-identity.md',
    'part-03-stack.md',
    'part-04-architecture.md',
    'part-05-components.md',
    'part-06-data.md',
    'part-07-device.md',
    'part-08-backend.md',
    'part-09-flutter.md',
    'part-10-ui.md',
    'part-11-media-ai.md',
    'part-12-assets.md',
    'part-13-build.md',
    'part-14-plan.md',
    'part-15-verification.md',
    'part-16-risks.md',
    'part-17-appendix-a.md',
    'part-18-appendix-b.md',
    'part-19-appendix-c.md',
    'part-20-appendix-d.md'
];

// Helper: parse inline markdown text into TextRun array
function parseInline(text, baseOptions = {}) {
    const runs = [];
    let remaining = text;

    while (remaining.length > 0) {
        // Match bold **text**
        const boldMatch = remaining.match(/^(\*\*|__)(.*?)\1/);
        if (boldMatch) {
            runs.push(new TextRun({
                text: boldMatch[2],
                bold: true,
                ...baseOptions
            }));
            remaining = remaining.substring(boldMatch[0].length);
            continue;
        }

        // Match inline code `code`
        const codeMatch = remaining.match(/^`([^`]+)`/);
        if (codeMatch) {
            runs.push(new TextRun({
                text: codeMatch[1],
                font: 'Consolas',
                size: 19, // 9.5pt
                color: '991B1B', // dark red/maroon
                ...baseOptions
            }));
            remaining = remaining.substring(codeMatch[0].length);
            continue;
        }

        // Match italic *text*
        const italicMatch = remaining.match(/^(\*|_)(.*?)\1/);
        if (italicMatch) {
            runs.push(new TextRun({
                text: italicMatch[2],
                italics: true,
                ...baseOptions
            }));
            remaining = remaining.substring(italicMatch[0].length);
            continue;
        }

        // Find next token position
        const nextSpecial = remaining.search(/(\*\*|__|`|\*|_)/);
        if (nextSpecial === -1) {
            runs.push(new TextRun({
                text: remaining,
                ...baseOptions
            }));
            break;
        } else if (nextSpecial === 0) {
            // Stray character
            runs.push(new TextRun({
                text: remaining[0],
                ...baseOptions
            }));
            remaining = remaining.substring(1);
        } else {
            runs.push(new TextRun({
                text: remaining.substring(0, nextSpecial),
                ...baseOptions
            }));
            remaining = remaining.substring(nextSpecial);
        }
    }

    if (runs.length === 0) {
        runs.push(new TextRun({ text: '', ...baseOptions }));
    }
    return runs;
}

// Convert markdown table lines to docx Table
function parseMarkdownTable(tableLines) {
    if (tableLines.length < 2) return null;

    // Filter out separator lines (|:---|:---|)
    const rows = [];
    let isHeader = true;

    for (const line of tableLines) {
        if (/^\|?\s*[-:]+[-| :]*\|?$/.test(line)) {
            isHeader = false;
            continue;
        }

        const rawCells = line.split('|');
        if (line.startsWith('|')) rawCells.shift();
        if (line.endsWith('|')) rawCells.pop();

        const cells = rawCells.map(c => c.trim());
        rows.push({ cells, isHeader });
    }

    if (rows.length === 0) return null;

    const numCols = Math.max(...rows.map(r => r.cells.length));
    const colWidth = Math.floor(9400 / numCols); // total width ~9400 twips (A4 with 2.2cm margins)

    const tableRows = rows.map((row, rIdx) => {
        const isHead = rIdx === 0;
        const tableCells = [];

        for (let cIdx = 0; cIdx < numCols; cIdx++) {
            const cellText = row.cells[cIdx] || '';
            const runs = parseInline(cellText, {
                font: isHead ? 'Calibri' : 'Calibri',
                size: 18, // 9 pt
                bold: isHead,
                color: isHead ? 'FFFFFF' : '1F2937'
            });

            tableCells.push(new TableCell({
                width: { size: colWidth, type: WidthType.DXA },
                shading: {
                    fill: isHead ? '1E293B' : (rIdx % 2 === 1 ? 'FFFFFF' : 'F8FAFC'),
                    type: ShadingType.CLEAR
                },
                margins: { top: 120, bottom: 120, left: 160, right: 160 },
                borders: {
                    top: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' },
                    bottom: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' },
                    left: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' },
                    right: { style: BorderStyle.SINGLE, size: 4, color: 'CBD5E1' }
                },
                children: [
                    new Paragraph({
                        children: runs,
                        spacing: { before: 40, after: 40 }
                    })
                ]
            }));
        }

        return new TableRow({
            children: tableCells,
            tableHeader: isHead
        });
    });

    return new Table({
        rows: tableRows,
        width: { size: 9400, type: WidthType.DXA }
    });
}

function parseMarkdownToDocx(mdContent, isFirstPart) {
    const lines = mdContent.split('\n');
    const elements = [];
    let i = 0;

    while (i < lines.length) {
        const line = lines[i];

        // Empty line
        if (line.trim().length === 0) {
            i++;
            continue;
        }

        // Horizontal rule
        if (/^---+\s*$/.test(line)) {
            i++;
            continue;
        }

        // Code block (fenced)
        if (line.trim().startsWith('```')) {
            const codeLines = [];
            i++;
            while (i < lines.length && !lines[i].trim().startsWith('```')) {
                codeLines.push(lines[i]);
                i++;
            }
            if (i < lines.length) i++; // skip closing ```

            // Add code block paragraphs
            for (const cLine of codeLines) {
                elements.push(new Paragraph({
                    children: [
                        new TextRun({
                            text: cLine.length > 0 ? cLine : ' ',
                            font: 'Consolas',
                            size: 17, // 8.5 pt
                            color: '0F172A'
                        })
                    ],
                    shading: {
                        fill: 'F1F5F9',
                        type: ShadingType.CLEAR
                    },
                    spacing: { before: 20, after: 20, line: 240 }
                }));
            }
            // Add a small spacing paragraph after code block
            elements.push(new Paragraph({ spacing: { before: 60, after: 60 } }));
            continue;
        }

        // Table block
        if (line.trim().startsWith('|')) {
            const tableLines = [];
            while (i < lines.length && lines[i].trim().startsWith('|')) {
                tableLines.push(lines[i]);
                i++;
            }
            const table = parseMarkdownTable(tableLines);
            if (table) {
                elements.push(table);
                elements.push(new Paragraph({ spacing: { before: 80, after: 80 } }));
            }
            continue;
        }

        // Heading 1 (# ...)
        if (line.startsWith('# ')) {
            const headingText = line.substring(2).trim();
            elements.push(new Paragraph({
                text: headingText,
                heading: HeadingLevel.HEADING_1,
                spacing: { before: isFirstPart && elements.length === 0 ? 0 : 360, after: 180 }
            }));
            i++;
            continue;
        }

        // Heading 2 (## ...)
        if (line.startsWith('## ')) {
            const headingText = line.substring(3).trim();
            elements.push(new Paragraph({
                text: headingText,
                heading: HeadingLevel.HEADING_2,
                spacing: { before: 280, after: 140 }
            }));
            i++;
            continue;
        }

        // Heading 3 (### ...)
        if (line.startsWith('### ')) {
            const headingText = line.substring(4).trim();
            elements.push(new Paragraph({
                text: headingText,
                heading: HeadingLevel.HEADING_3,
                spacing: { before: 220, after: 100 }
            }));
            i++;
            continue;
        }

        // Heading 4 (#### ...)
        if (line.startsWith('#### ')) {
            const headingText = line.substring(5).trim();
            elements.push(new Paragraph({
                children: [
                    new TextRun({
                        text: headingText,
                        bold: true,
                        size: 22, // 11pt
                        color: '1E293B',
                        font: 'Calibri'
                    })
                ],
                spacing: { before: 180, after: 80 }
            }));
            i++;
            continue;
        }

        // Bullet item (- ... or * ...)
        if (/^\s*[-*]\s+/.test(line)) {
            const clean = line.replace(/^\s*[-*]\s+/, '');
            const isTask = clean.startsWith('[ ] ') || clean.startsWith('[x] ');
            const displayText = isTask ? (clean.startsWith('[x] ') ? '☑ ' : '☐ ') + clean.substring(4) : clean;
            elements.push(new Paragraph({
                children: parseInline(displayText, { font: 'Calibri', size: 21, color: '1F2937' }),
                bullet: { level: 0 },
                spacing: { before: 40, after: 40 }
            }));
            i++;
            continue;
        }

        // Numbered list (1. ...)
        if (/^\s*\d+\.\s+/.test(line)) {
            const clean = line.replace(/^\s*\d+\.\s+/, '');
            elements.push(new Paragraph({
                children: parseInline(clean, { font: 'Calibri', size: 21, color: '1F2937' }),
                spacing: { before: 50, after: 50 }
            }));
            i++;
            continue;
        }

        // Regular Paragraph
        elements.push(new Paragraph({
            children: parseInline(line, { font: 'Calibri', size: 21, color: '1F2937' }), // 10.5 pt
            spacing: { before: 60, after: 60, line: 260 }
        }));
        i++;
    }

    return elements;
}

async function buildDocument() {
    console.log('Building iPulse_5.9.1.17_ECHO_Rebuild_Spec.docx...');

    const allChildren = [];

    // Table of contents fallback entries
    const manualTocItems = [
        { title: '1. Cover & Verification Provenance', page: '1' },
        { title: '2. Executive Summary & System Overview', page: '3' },
        { title: '3. Application Identity & Resource Budget', page: '5' },
        { title: '4. Runtime Environment & Dependency Stack', page: '8' },
        { title: '5. System Architecture & Dual-Shell Design', page: '11' },
        { title: '6. Android Components & Security Permissions', page: '16' },
        { title: '7. Persistence Architecture & Room Schemas', page: '21' },
        { title: '8. Wearable BLE Subsystem & Driver Registry', page: '27' },
        { title: '9. Backend Services & Cloud Ingest API', page: '34' },
        { title: '10. Flutter Presentation & Platform Channels', page: '39' },
        { title: '11. UI Screen Map & AMOLED Theming', page: '44' },
        { title: '12. Media Processing & Multimodal AI', page: '49' },
        { title: '13. Asset Inventory & Licensing Audit', page: '53' },
        { title: '14. Target Gradle Build System & R8 Optimization', page: '56' },
        { title: '15. ECHO Phased Rebuild Plan and Agent Handoffs (P0–P8)', page: '60' },
        { title: '16. Verification Matrix & Parity Certification', page: '68' },
        { title: '17. Risk Analysis & Technical Gaps', page: '72' },
        { title: '18. Appendix A — Architectural Evidence Audit Log', page: '75' },
        { title: '19. Appendix B — Verification Counts & Discrepancies', page: '80' },
        { title: '20. Appendix C — Exact Reproduction Commands', page: '82' },
        { title: '21. Appendix D — Technical Glossary', page: '85' }
    ];

    for (let idx = 0; idx < PARTS.length; idx++) {
        const partFile = PARTS[idx];
        const partPath = path.join(DOCBUILD_DIR, partFile);
        console.log(`Processing [${idx + 1}/${PARTS.length}] ${partFile}...`);

        if (!fs.existsSync(partPath)) {
            console.error(`Missing part file: ${partPath}`);
            continue;
        }

        const mdContent = fs.readFileSync(partPath, 'utf8');
        const elements = parseMarkdownToDocx(mdContent, idx === 0);

        allChildren.push(...elements);

        // Insert Table of Contents after Cover (Part 00)
        if (idx === 0) {
            allChildren.push(new Paragraph({
                children: [new PageBreak()]
            }));

            allChildren.push(new Paragraph({
                text: 'Table of Contents',
                heading: HeadingLevel.HEADING_1,
                spacing: { before: 200, after: 200 }
            }));

            // Table of Contents Rows
            const tocRows = manualTocItems.map((item, index) => {
                return new TableRow({
                    children: [
                        new TableCell({
                            width: { size: 8200, type: WidthType.DXA },
                            borders: {
                                top: { style: BorderStyle.NONE },
                                bottom: { style: BorderStyle.DOTTED, size: 2, color: 'CBD5E1' },
                                left: { style: BorderStyle.NONE },
                                right: { style: BorderStyle.NONE }
                            },
                            children: [
                                new Paragraph({
                                    children: [
                                        new TextRun({
                                            text: item.title,
                                            font: 'Calibri',
                                            size: 21,
                                            bold: index < 5
                                        })
                                    ],
                                    spacing: { before: 40, after: 40 }
                                })
                            ]
                        }),
                        new TableCell({
                            width: { size: 1200, type: WidthType.DXA },
                            borders: {
                                top: { style: BorderStyle.NONE },
                                bottom: { style: BorderStyle.DOTTED, size: 2, color: 'CBD5E1' },
                                left: { style: BorderStyle.NONE },
                                right: { style: BorderStyle.NONE }
                            },
                            children: [
                                new Paragraph({
                                    alignment: AlignmentType.RIGHT,
                                    children: [
                                        new TextRun({
                                            text: item.page,
                                            font: 'Calibri',
                                            size: 21,
                                            color: '64748B'
                                        })
                                    ],
                                    spacing: { before: 40, after: 40 }
                                })
                            ]
                        })
                    ]
                });
            });

            allChildren.push(new Table({
                rows: tocRows,
                width: { size: 9400, type: WidthType.DXA }
            }));

            allChildren.push(new Paragraph({
                children: [new PageBreak()]
            }));
        } else if (idx < PARTS.length - 1) {
            // Add page break between major parts
            allChildren.push(new Paragraph({
                children: [new PageBreak()]
            }));
        }
    }

    // Configure document styles and sections
    const doc = new Document({
        styles: {
            default: {
                document: {
                    run: {
                        font: 'Calibri',
                        size: 21, // 10.5 pt
                        color: '1F2937'
                    }
                }
            },
            heading1: {
                run: {
                    font: 'Calibri',
                    size: 40, // 20 pt
                    bold: true,
                    color: '0F172A'
                }
            },
            heading2: {
                run: {
                    font: 'Calibri',
                    size: 30, // 15 pt
                    bold: true,
                    color: '1E293B'
                }
            },
            heading3: {
                run: {
                    font: 'Calibri',
                    size: 24, // 12 pt
                    bold: true,
                    color: '334155'
                }
            }
        },
        sections: [
            {
                properties: {
                    page: {
                        size: {
                            width: 11906, // A4 width in twips (210 mm)
                            height: 16838 // A4 height in twips (297 mm)
                        },
                        margin: {
                            top: 1247,    // 2.2 cm in twips
                            bottom: 1247,
                            left: 1247,
                            right: 1247
                        }
                    }
                },
                headers: {
                    default: new Header({
                        children: [
                            new Paragraph({
                                alignment: AlignmentType.RIGHT,
                                children: [
                                    new TextRun({
                                        text: 'iPulse 5.9.1.17 Analysis & ECHO Rebuild Plan | DRAFT v1.0',
                                        font: 'Calibri',
                                        size: 17, // 8.5 pt
                                        color: '64748B'
                                    })
                                ],
                                spacing: { after: 120 }
                            })
                        ]
                    })
                },
                footers: {
                    default: new Footer({
                        children: [
                            new Paragraph({
                                alignment: AlignmentType.RIGHT,
                                children: [
                                    new TextRun({
                                        text: 'Page ',
                                        font: 'Calibri',
                                        size: 18,
                                        color: '64748B'
                                    }),
                                    new TextRun({
                                        children: [PageNumber.CURRENT],
                                        font: 'Calibri',
                                        size: 18,
                                        color: '64748B'
                                    }),
                                    new TextRun({
                                        text: ' of ',
                                        font: 'Calibri',
                                        size: 18,
                                        color: '64748B'
                                    }),
                                    new TextRun({
                                        children: [PageNumber.TOTAL_PAGES],
                                        font: 'Calibri',
                                        size: 18,
                                        color: '64748B'
                                    })
                                ]
                            })
                        ]
                    })
                },
                children: allChildren
            }
        ]
    });

    console.log('Packing document into buffer...');
    const buffer = await Packer.toBuffer(doc);
    fs.writeFileSync(OUTPUT_FILE, buffer);
    console.log(`Successfully generated: ${OUTPUT_FILE} (${buffer.length} bytes)`);
}

buildDocument().catch(err => {
    console.error('Build failed:', err);
    process.exit(1);
});
