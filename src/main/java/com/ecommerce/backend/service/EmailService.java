package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // @Async — sends email in background thread, doesn't block order response
    @Async
    public void sendOrderConfirmation(String toEmail,
                                      OrderResponse order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom("shindeamey48@gmail.com", "ShopEasy");
            helper.setTo(toEmail);
            helper.setSubject("Order Confirmed — #" + order.getId()
                    + " | ShopEasy");
            helper.setText(buildEmailHtml(order), true); // true = HTML

            mailSender.send(message);
        } catch (Exception e) {
            // Email failure should never break the order flow
            System.err.println("Email send failed: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderShipped(String toEmail, OrderResponse order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom("shindeamey48@gmail.com", "ShopEasy");
            helper.setTo(toEmail);
            helper.setSubject("Order Shipped — #" + order.getId()
                    + " | ShopEasy");
            helper.setText(buildStatusEmailHtml(order, "Order Shipped!", "Great news! Your order is on its way.", "#2b6cb0"), true); // true = HTML

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }

    @Async
    public void sendOrderDelivered(String toEmail, OrderResponse order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom("shindeamey48@gmail.com", "ShopEasy");
            helper.setTo(toEmail);
            helper.setSubject("Order Delivered — #" + order.getId()
                    + " | ShopEasy");
            helper.setText(buildStatusEmailHtml(order, "Order Delivered!", "Your order has been successfully delivered. We hope you love it!", "#2f855a"), true); // true = HTML

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }

    private String buildStatusEmailHtml(OrderResponse order, String title, String message, String color) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:sans-serif;max-width:500px;margin:0 auto'>");
        sb.append("<h2 style='color:").append(color).append("'>").append(title).append("</h2>");
        sb.append("<p>Hi <strong>").append(order.getDeliveryName())
                .append("</strong>, ").append(message).append("</p>");
        sb.append("<p>Order ID: <strong>#").append(order.getId())
                .append("</strong></p>");

        sb.append("<h3>Items Details</h3><ul>");
        if (order.getItems() != null) {
            order.getItems().forEach(item ->
                    sb.append("<li>").append(item.getProductName())
                            .append(" × ").append(item.getQuantity())
                            .append("</li>")
            );
        }
        sb.append("</ul>");
        sb.append("<p>Delivery to: ")
                .append(order.getDeliveryStreet()).append(", ")
                .append(order.getDeliveryCity()).append("</p>");
        sb.append("<p style='color:#1d9e75'>Thank you for shopping with ShopEasy!</p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildEmailHtml(OrderResponse order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:sans-serif;max-width:500px;margin:0 auto'>");
        sb.append("<h2 style='color:#1a1a2e'>Order Confirmed!</h2>");
        sb.append("<p>Hi <strong>").append(order.getDeliveryName())
                .append("</strong>, your order has been placed successfully.</p>");
        sb.append("<p>Order ID: <strong>#").append(order.getId())
                .append("</strong></p>");

        sb.append("<h3>Items Ordered</h3><ul>");
        if (order.getItems() != null) {
            order.getItems().forEach(item ->
                    sb.append("<li>").append(item.getProductName())
                            .append(" × ").append(item.getQuantity())
                            .append(" — ₹").append(item.getItemTotal())
                            .append("</li>")
            );
        }
        sb.append("</ul>");
        sb.append("<p><strong>Total: ₹").append(order.getTotalAmount())
                .append("</strong></p>");
        sb.append("<p>Delivery to: ")
                .append(order.getDeliveryStreet()).append(", ")
                .append(order.getDeliveryCity()).append("</p>");
        sb.append("<p style='color:#1d9e75'>Thank you for shopping with ShopEasy!</p>");
        sb.append("</div>");
        return sb.toString();
    }
}