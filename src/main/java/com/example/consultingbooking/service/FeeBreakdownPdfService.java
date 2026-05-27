package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.BookingDtos;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FeeBreakdownPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] create(BookingDtos.FeeBreakdownResponse breakdown) {
        List<String> lines = new ArrayList<>();
        lines.add("Consultation Fee Breakdown");
        lines.add("");
        lines.add("Booking ID: " + breakdown.bookingId());
        lines.add("Specialist: " + safe(breakdown.specialistName()));
        lines.add("Topic: " + safe(breakdown.topic()));
        lines.add("Appointment: " + format(breakdown.startTime()) + " - " + format(breakdown.endTime()));
        lines.add("");
        lines.add("Pricing Summary");
        lines.add("Unit price: " + breakdown.feeCurrency() + " " + breakdown.unitPrice() + " / hour");
        lines.add("Duration: " + breakdown.durationMinutes() + " minutes");
        lines.add("Pricing multiplier: " + breakdown.pricingMultiplier());
        lines.add("Total fee: " + breakdown.feeCurrency() + " " + breakdown.totalPrice());
        lines.add("");
        lines.add("Cost Components");
        for (BookingDtos.FeeSegmentResponse component : breakdown.components()) {
            lines.add(component.label());
            lines.add("  Time: " + format(component.startTime()) + " - " + format(component.endTime()));
            lines.add("  Duration: " + component.durationMinutes() + " minutes | Multiplier: x" + component.multiplier());
            lines.add("  Amount: " + breakdown.feeCurrency() + " " + component.amount());
        }
        lines.add("");
        lines.add("Generated from the booking fee snapshot");
        lines.add("stored by the platform.");
        return buildPdf(lines);
    }

    private byte[] buildPdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT\n54 790 Td\n");
        appendText(content, lines.getFirst(), 18);
        for (int index = 1; index < lines.size(); index++) {
            content.append("0 -20 Td\n");
            appendText(content, lines.get(index), 10);
        }
        content.append("ET\n");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<byte[]> objects = List.of(
                ascii("<< /Type /Catalog /Pages 2 0 R >>"),
                ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> /Contents 4 0 R >>"),
                stream(contentBytes),
                ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
                ascii("<< /Type /Font /Subtype /Type0 /BaseFont /STSong-Light /Encoding /UniGB-UCS2-H /DescendantFonts [7 0 R] >>"),
                ascii("<< /Type /Font /Subtype /CIDFontType0 /BaseFont /STSong-Light /CIDSystemInfo << /Registry (Adobe) /Ordering (GB1) /Supplement 4 >> >>")
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, ascii("%PDF-1.4\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(output.size());
            write(output, ascii((index + 1) + " 0 obj\n"));
            write(output, objects.get(index));
            write(output, ascii("\nendobj\n"));
        }
        int xrefOffset = output.size();
        write(output, ascii("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (Integer offset : offsets) {
            write(output, ascii(String.format("%010d 00000 n \n", offset)));
        }
        write(output, ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xrefOffset + "\n%%EOF\n"));
        return output.toByteArray();
    }

    private void appendText(StringBuilder content, String value, int size) {
        boolean unicode = safe(value).chars().anyMatch(character -> character > 127);
        content.append(unicode ? "/F2 " : "/F1 ")
                .append(size)
                .append(" Tf\n")
                .append(unicode ? unicodePdfString(value) : asciiPdfString(value))
                .append(" Tj\n");
    }

    private String asciiPdfString(String value) {
        return "(" + safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)") + ")";
    }

    private String unicodePdfString(String value) {
        byte[] encoded = safe(value).getBytes(StandardCharsets.UTF_16BE);
        StringBuilder result = new StringBuilder("<");
        for (byte item : encoded) {
            result.append(String.format("%02X", item & 0xff));
        }
        return result.append(">").toString();
    }

    private byte[] stream(byte[] bytes) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        write(result, ascii("<< /Length " + bytes.length + " >>\nstream\n"));
        write(result, bytes);
        write(result, ascii("endstream"));
        return result.toByteArray();
    }

    private byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private void write(ByteArrayOutputStream target, byte[] value) {
        target.writeBytes(value);
    }

    private String format(java.time.LocalDateTime value) {
        return value.format(DATE_TIME_FORMAT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
