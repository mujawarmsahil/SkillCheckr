package com.skillcheckr.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.skillcheckr.model.Exam;
import com.skillcheckr.model.QuestionDTO;

@Component
public class QuestionBankPdfGenerator {

    private static final float LEFT_MARGIN = 50;
    private static final float TOP_MARGIN = 750;
    private static final float LINE_HEIGHT = 16;
    private static final float CONTENT_WIDTH = 500;

    public byte[] generate(Exam exam, List<QuestionDTO> questions) {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PageWriter writer = new PageWriter(document, exam.getExamName());
            writer.writeTitle();

            for (int index = 0; index < questions.size(); index++) {
                QuestionDTO question = questions.get(index);
                writer.writeQuestion(index + 1, question);
            }
            writer.closePage();
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate the student question-bank PDF", ex);
        }
    }

    private static final class PageWriter {
        private final PDDocument document;
        private final String title;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private PageWriter(PDDocument document, String title) throws IOException {
            this.document = document;
            this.title = title;
            startPage();
        }

        private void writeTitle() throws IOException {
            writeLine(title, true, 18, 24);
            writeLine("Student question bank", false, 11, 22);
        }

        private void writeQuestion(int number, QuestionDTO question) throws IOException {
            List<String> lines = wrap(number + ". " + question.getQuestion(), false, 11, CONTENT_WIDTH);
            int optionCount = countOptions(question);
            int requiredLines = lines.size() + optionCount + 2;
            if (y - requiredLines * LINE_HEIGHT < 55) {
                startPage();
            }

            for (String line : lines) {
                writeLine(line, false, 11, LINE_HEIGHT);
            }

            String[] options = {
                question.getOption1(), question.getOption2(), question.getOption3(), question.getOption4()
            };
            char label = 'A';
            for (String option : options) {
                if (option != null && !option.isBlank()) {
                    List<String> optionLines = wrap(label++ + ") " + option, false, 10, CONTENT_WIDTH - 20);
                    for (String line : optionLines) {
                        writeLine("   " + line, false, 10, LINE_HEIGHT);
                    }
                }
            }
            writeLine("", false, 10, LINE_HEIGHT);
        }

        private int countOptions(QuestionDTO question) {
            int count = 0;
            if (question.getOption1() != null && !question.getOption1().isBlank()) count++;
            if (question.getOption2() != null && !question.getOption2().isBlank()) count++;
            if (question.getOption3() != null && !question.getOption3().isBlank()) count++;
            if (question.getOption4() != null && !question.getOption4().isBlank()) count++;
            return count;
        }

        private void writeLine(String value, boolean bold, float fontSize, float advance) throws IOException {
            PDType1Font font = new PDType1Font(bold
                    ? Standard14Fonts.FontName.HELVETICA_BOLD
                    : Standard14Fonts.FontName.HELVETICA);
            List<String> lines = wrap(value, bold, fontSize, CONTENT_WIDTH);
            for (String line : lines) {
                if (y < 55) {
                    startPage();
                }
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(LEFT_MARGIN, y);
                stream.showText(line);
                stream.endText();
                y -= advance;
            }
        }

        private List<String> wrap(String value, boolean bold, float fontSize, float maxWidth) throws IOException {
            PDType1Font font = new PDType1Font(bold
                    ? Standard14Fonts.FontName.HELVETICA_BOLD
                    : Standard14Fonts.FontName.HELVETICA);
            String normalized = toSupportedText(value, font);
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : normalized.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.getStringWidth(candidate) * fontSize / 1000 > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            if (lines.isEmpty()) {
                lines.add("");
            }
            return lines;
        }

        private String toSupportedText(String value, PDType1Font font) {
            StringBuilder result = new StringBuilder();
            value.codePoints().forEach(codePoint -> {
                String character = new String(Character.toChars(codePoint));
                try {
                    font.encode(character);
                    result.append(character);
                } catch (IllegalArgumentException ex) {
                    result.append('?');
                } catch (IOException ex) {
                    throw new IllegalStateException("Unable to encode PDF text", ex);
                }
            });
            return result.toString();
        }

        private void startPage() throws IOException {
            closePage();
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = TOP_MARGIN;
            if (document.getNumberOfPages() > 1) {
                writeLine(title, true, 12, 22);
            }
        }

        private void closePage() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
