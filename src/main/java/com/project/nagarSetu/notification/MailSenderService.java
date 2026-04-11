package com.project.nagarSetu.notification;

import com.project.nagarSetu.event.SendPinEvents;
import com.project.nagarSetu.event.SendPinJobEvents;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailSenderService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @EventListener
    public void sendPinMail(SendPinEvents events) {
        try {
            log.trace("Trying to send a mail to {}", events.email());
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            helper.setTo(events.email());
            helper.setFrom(fromEmail);
            helper.setSubject("Email Verification Code");

            String mail = getTextMail(events);
            helper.setText(mail, true);

            mailSender.send(mailMessage);
            log.info("Mail send pin to email : {}({})", events.email(), events.code());

        } catch (MessagingException e) {
            log.error("Mail Sender Error", e);
        }

    }

    public String getTextMail(SendPinEvents events) {
        String email = events.email();
        String code = events.code();

        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Verification Code - NagarSetu</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: Arial, Helvetica, sans-serif; background-color: #f5f5f5;\">"
                +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5; padding: 20px 0;\">"
                +
                "<tr>" +
                "<td align=\"center\">" +
                "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);\">"
                +
                "<tr>" +
                "<td style=\"background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%); padding: 30px; text-align: center;\">"
                +
                "<h1 style=\"margin: 0; color: #ffffff; font-size: 32px; font-weight: 700;\">" +
                "Nagar<span style=\"color: #ffffff;\">Setu</span>" +
                "</h1>" +
                "<p style=\"margin: 8px 0 0 0; color: #E3F2FD; font-size: 14px; font-weight: 500;\">Government of India Initiative</p>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"padding: 40px 30px;\">" +
                "<h2 style=\"margin: 0 0 20px 0; color: #212121; font-size: 24px; font-weight: 600;\">Account Verification</h2>"
                +
                "<p style=\"margin: 0 0 25px 0; color: #424242; font-size: 16px; line-height: 1.6;\">Dear " + email
                + ",</p>" +
                "<p style=\"margin: 0 0 25px 0; color: #424242; font-size: 16px; line-height: 1.6;\">" +
                "Thank you for registering with NagarSetu. To complete your account verification, please use the following One-Time Password (OTP):"
                +
                "</p>" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr>" +
                "<td align=\"center\" style=\"padding: 25px 0;\">" +
                "<div style=\"background-color: #E3F2FD; border: 2px solid #1976D2; border-radius: 8px; padding: 20px 40px; display: inline-block;\">"
                +
                "<span style=\"font-size: 36px; font-weight: 700; color: #1976D2; letter-spacing: 8px; font-family: 'Courier New', monospace;\">"
                + code + "</span>" +
                "</div>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<p style=\"margin: 0 0 15px 0; color: #424242; font-size: 16px; line-height: 1.6;\">" +
                "This verification code will expire in <strong style=\"color: #D32F2F;\">10 minutes</strong>." +
                "</p>" +
                "<p style=\"margin: 0 0 20px 0; color: #757575; font-size: 14px; line-height: 1.6;\">" +
                "If you did not request this code, please ignore this email or contact our support team immediately." +
                "</p>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    @EventListener
    public void sendJobMail(SendPinJobEvents events) {
        try {
            log.trace("Trying to send a mail to {}", events.email());
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            helper.setTo(events.email());
            helper.setFrom(fromEmail);
            helper.setSubject("Email Verification Code");

            SendPinEvents eventss = new SendPinEvents(events.email() , events.code());
            String mail = getTextMail(eventss);
            helper.setText(mail, true);

            mailSender.send(mailMessage);
            log.info("Mail send pin to email : {}({})", events.email(), events.code());

        } catch (MessagingException e) {
            log.error("Mail Sender Error", e);
        }

    }

    @EventListener
    public void sendIssueAssignedMail(com.project.nagarSetu.event.IssueAssignedEvent events) {
        try {
            log.trace("Sending issue assignment mail to {}", events.email());
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            helper.setTo(events.email());
            helper.setFrom(fromEmail);
            helper.setSubject(events.subject());
            helper.setText(events.body(), true);

            mailSender.send(mailMessage);
            log.info("Issue assignment mail sent to {}", events.email());

        } catch (Exception e) {
            log.error("Issue assignment mail failed to send to {}", events.email(), e);
        }

    }

    @EventListener
    public void sendIssueAssignedDataMail(com.project.nagarSetu.event.IssueAssignedDataEvent event) {
        try {
            log.trace("Building assignment email for {} (role={})", event.recipientEmail(), event.recipientRole());
            MimeMessage mailMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true, "UTF-8");
            helper.setTo(event.recipientEmail());
            helper.setFrom(fromEmail);

            String subject = switch (event.recipientRole()) {
                case "SUPERVISOR" -> "New Issue Assigned / Worker Assigned";
                case "SUPERVISOR_DONE" -> "Issue Completed in your Jurisdiction";
                case "WORKER" -> "You have been assigned a new issue";
                case "WORKER_DONE" -> "Issue successfully resolved";
                default -> "Issue Notification";
            };

            helper.setSubject(subject);

            StringBuilder body = new StringBuilder();
            body.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>NagarSetu Notification</title></head>")
                .append("<body style=\"margin: 0; padding: 0; font-family: Arial, Helvetica, sans-serif; background-color: #f5f5f5;\">")
                .append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5; padding: 20px 0;\"><tr><td align=\"center\">")
                .append("<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);\">")
                .append("<tr><td style=\"background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%); padding: 30px; text-align: center;\">")
                .append("<h1 style=\"margin: 0; color: #ffffff; font-size: 32px; font-weight: 700;\">Nagar<span style=\"color: #ffffff;\">Setu</span></h1>")
                .append("<p style=\"margin: 8px 0 0 0; color: #E3F2FD; font-size: 14px; font-weight: 500;\">Government of India Initiative</p>")
                .append("</td></tr>")
                .append("<tr><td style=\"padding: 40px 30px;\">")
                .append("<h2 style=\"margin: 0 0 20px 0; color: #212121; font-size: 24px; font-weight: 600;\">").append(subject).append("</h2>")
                .append("<p style=\"margin: 0 0 25px 0; color: #424242; font-size: 16px; line-height: 1.6;\">Dear User,</p>")
                
                .append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #FAFAFA; border-radius: 8px; padding: 20px; border: 1px solid #E0E0E0; margin-bottom: 25px;\">")
                .append("<tr><td colspan=\"2\" style=\"padding-bottom: 15px;\">")
                .append("<span style=\"color: #1976D2; font-size: 14px; font-weight: 600; text-transform: uppercase;\">Issue Summary</span></td></tr>")
                
                .append("<tr><td width=\"100\" style=\"padding-bottom: 15px; color: #616161; font-size: 14px;\"><strong>Title:</strong></td>")
                .append("<td style=\"padding-bottom: 15px; color: #212121; font-size: 14px;\">").append(event.title()).append("</td></tr>")
                
                .append("<tr><td width=\"100\" style=\"padding-bottom: 15px; color: #616161; font-size: 14px;\"><strong>Details:</strong></td>")
                .append("<td style=\"padding-bottom: 15px; color: #212121; font-size: 14px;\">").append(event.description()).append("</td></tr>")
                
                .append("<tr><td width=\"100\" style=\"padding-bottom: 15px; color: #616161; font-size: 14px;\"><strong>Priority:</strong></td>")
                .append("<td style=\"padding-bottom: 15px;\"><span style=\"background-color: #ffebee; color: #c62828; padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: bold;\">")
                .append(event.criticality()).append("</span></td></tr>");
                
            if (event.url() != null && !event.url().isEmpty() && !event.url().equals("null")) {
                body.append("<tr><td width=\"100\" style=\"padding-bottom: 15px; color: #616161; font-size: 14px;\"><strong>Proof:</strong></td>")
                    .append("<td style=\"padding-bottom: 15px; color: #1976D2; font-size: 14px;\"><a href=\"").append(event.url()).append("\">View Image</a></td></tr>");
            }

            if (event.recipientRole().startsWith("SUPERVISOR")) {
                body.append("<tr><td width=\"100\" style=\"padding-bottom: 15px; color: #616161; font-size: 14px;\"><strong>Worker:</strong></td>")
                    .append("<td style=\"padding-bottom: 15px; color: #212121; font-size: 14px;\">")
                    .append(event.workerName() == null ? "N/A" : event.workerName()).append("</td></tr>");
            }

            if (event.recipientRole().startsWith("WORKER")) {
                body.append("<tr><td width=\"100\" style=\"padding-bottom: 15px; color: #616161; font-size: 14px;\"><strong>Area Head:</strong></td>")
                    .append("<td style=\"padding-bottom: 15px; color: #212121; font-size: 14px;\">")
                    .append(event.supervisorName() == null ? "N/A" : event.supervisorName()).append("</td></tr>");
            }

            body.append("</table>")
                .append("<p style=\"margin: 0; color: #424242; font-size: 16px; line-height: 1.6;\">You can log into the dashboard to view further options and tracking details.</p>")
                .append("</td></tr>")
                .append("<tr><td style=\"background-color: #FAFAFA; padding: 25px 30px; border-top: 1px solid #E0E0E0;\">")
                .append("<p style=\"margin: 0 0 15px 0; color: #424242; font-size: 14px; font-weight: 600;\">NagarSetu Team</p>")
                .append("</td></tr></table></td></tr></table></body></html>");

            helper.setText(body.toString(), true);
            mailSender.send(mailMessage);
            log.info("Assignment email sent to {}", event.recipientEmail());
        } catch (Exception e) {
            log.error("Assignment email failed for {}", event.recipientEmail(), e);
        }
    }

    public String getWelcomeMail(String email, String code, String role) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Welcome to NagarSetu</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: Arial, Helvetica, sans-serif; background-color: #f5f5f5;\">"
                +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5; padding: 20px 0;\">"
                +
                "<tr>" +
                "<td align=\"center\">" +
                "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);\">"
                +
                "<tr>" +
                "<td style=\"background: linear-gradient(135deg, #1976D2 0%, #1565C0 100%); padding: 30px; text-align: center;\">"
                +
                "<h1 style=\"margin: 0; color: #ffffff; font-size: 32px; font-weight: 700;\">" +
                "Nagar<span style=\"color: #ffffff;\">Setu</span>" +
                "</h1>" +
                "<p style=\"margin: 8px 0 0 0; color: #E3F2FD; font-size: 14px; font-weight: 500;\">Government of India Initiative</p>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"padding: 40px 30px;\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr>" +
                "<td align=\"center\" style=\"padding-bottom: 20px;\">" +
                "<div style=\"width: 80px; height: 80px; background-color: #E3F2FD; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center;\">"
                +
                "<span style=\"font-size: 40px; line-height: 1;\">👋</span>" +
                "</div>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<h2 style=\"margin: 0 0 20px 0; color: #212121; font-size: 24px; font-weight: 600; text-align: center;\">Welcome to the Community!</h2>"
                +
                "<p style=\"margin: 0 0 20px 0; color: #424242; font-size: 16px; line-height: 1.6;\">Dear " + email
                + ",</p>" +
                "<p style=\"margin: 0 0 25px 0; color: #424242; font-size: 16px; line-height: 1.6;\">" +
                "Thank you for joining <strong>NagarSetu</strong> as a <strong>" + role
                + "</strong>. You have taken the first step towards building a cleaner, safer, and smarter city. We are thrilled to have you as a partner in civic progress."
                +
                "</p>" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #FAFAFA; border-radius: 8px; padding: 20px; border: 1px solid #E0E0E0; margin-bottom: 25px;\">"
                +
                "<tr>" +
                "<td colspan=\"2\" style=\"padding-bottom: 15px;\">" +
                "<span style=\"color: #1976D2; font-size: 14px; font-weight: 600; text-transform: uppercase;\">What you can do now:</span>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td width=\"30\" valign=\"top\" style=\"padding-bottom: 15px;\">📸</td>" +
                "<td style=\"padding-bottom: 15px;\">" +
                "<strong style=\"color: #212121; font-size: 14px;\">Report Issues</strong>" +
                "<div style=\"color: #616161; font-size: 13px; margin-top: 4px;\">Spot a pothole or garbage? Snap a picture and report it instantly.</div>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td width=\"30\" valign=\"top\" style=\"padding-bottom: 15px;\">📍</td>" +
                "<td style=\"padding-bottom: 15px;\">" +
                "<strong style=\"color: #212121; font-size: 14px;\">Track Progress</strong>" +
                "<div style=\"color: #616161; font-size: 13px; margin-top: 4px;\">Get real-time updates as authorities resolve your complaints.</div>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td width=\"30\" valign=\"top\">🏆</td>" +
                "<td>" +
                "<strong style=\"color: #212121; font-size: 14px;\">Earn Rewards</strong>" +
                "<div style=\"color: #616161; font-size: 13px; margin-top: 4px;\">Collect points for every resolved issue and climb the leaderboard.</div>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #E3F2FD; border-left: 4px solid #1976D2; padding: 15px; border-radius: 4px; margin-bottom: 25px;\">"
                +
                "<tr>" +
                "<td>" +
                "<p style=\"margin: 0 0 8px 0; color: #1565C0; font-size: 14px; font-weight: 600;\">Complete Your Profile</p>"
                +
                "<p style=\"margin: 0; color: #0D47A1; font-size: 13px; line-height: 1.5;\">To ensure your reports are prioritized, please make sure your contact details and ward location are accurate.</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<p style=\"margin: 0 0 20px 0; color: #424242; font-size: 16px; line-height: 1.6;\">Ready to make a difference? Let's get started.</p>"
                +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top: 30px;\">" +
                "<tr>" +
                "<td align=\"center\">" +
                "<a href=\"#\" style=\"display: inline-block; background-color: #1976D2; color: #ffffff; text-decoration: none; padding: 14px 35px; border-radius: 6px; font-size: 15px; font-weight: 600;\">Get Started</a>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"background-color: #FAFAFA; padding: 25px 30px; border-top: 1px solid #E0E0E0;\">" +
                "<p style=\"margin: 0 0 8px 0; color: #757575; font-size: 13px; line-height: 1.5;\">Together for a better tomorrow,</p>"
                +
                "<p style=\"margin: 0 0 15px 0; color: #424242; font-size: 14px; font-weight: 600;\">NagarSetu Team</p>"
                +
                "<p style=\"margin: 0; color: #9E9E9E; font-size: 12px; line-height: 1.5;\">Ministry of Urban Development | Government of India</p>"
                +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style=\"background-color: #263238; padding: 20px 30px; text-align: center;\">" +
                "<p style=\"margin: 0 0 8px 0; color: #B0BEC5; font-size: 12px;\">© 2026 NagarSetu. All rights reserved.</p>"
                +
                "<p style=\"margin: 0; color: #78909C; font-size: 11px;\">You received this email because you signed up for NagarSetu.</p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top: 15px;\">" +
                "<tr>" +
                "<td align=\"center\">" +
                "<p style=\"margin: 0; color: #9E9E9E; font-size: 11px; line-height: 1.5;\">If you have any questions, contact us at <a href=\"mailto:nagarsetu.care@gmail.com\" style=\"color: #1976D2; text-decoration: none;\">nagarsetu.care@gmail.com</a></p>"
                +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "</body>" +
                "</html>";
    }

    String getRolePin(SendPinJobEvents events){
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Verification Code - NagarSetu</title>" +
                "</head>" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;\">" +

                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5; padding: 40px 0;\">" +
                "<tr>" +
                "<td align=\"center\">" +

                "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); border: 1px solid #e0e0e0;\">" +

                "<tr>" +
                "<td style=\"background: linear-gradient(135deg, #1976D2 0%, #0D47A1 100%); padding: 35px; text-align: center;\">" +
                "<h1 style=\"margin: 0; color: #ffffff; font-size: 32px; font-weight: 700; letter-spacing: 1px;\">" +
                "Nagar<span style=\"color: #E3F2FD;\">Setu</span>" +
                "</h1>" +
                "<p style=\"margin: 8px 0 0 0; color: #BBDEFB; font-size: 14px; font-weight: 500; letter-spacing: 0.5px;\">Government of India Initiative</p>" +
                "</td>" +
                "</tr>" +

                "<tr>" +
                "<td style=\"padding: 40px 30px;\">" +

                "<h2 style=\"margin: 0 0 25px 0; color: #2D3436; font-size: 24px; font-weight: 700; text-align: center;\">Account Verification</h2>" +

                "<p style=\"margin: 0 0 10px 0; color: #424242; font-size: 16px; line-height: 1.6;\">Dear " + events.email() + ",</p>" +

                "<div style=\"margin-bottom: 25px;\">" +
                "<span style=\"font-size: 14px; color: #757575; font-weight: 500;\">Registering as: </span>" +
                "<span style=\"display: inline-block; background-color: #E3F2FD; color: #1565C0; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; border: 1px solid #BBDEFB;\">" +
                events.role() +
                "</span>" +
                "</div>" +

                "<p style=\"margin: 0 0 25px 0; color: #636E72; font-size: 16px; line-height: 1.6;\">" +
                "Thank you for joining NagarSetu. To complete your account verification and access the portal, please use the following One-Time Password (OTP):" +
                "</p>" +

                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr>" +
                "<td align=\"center\" style=\"padding: 20px 0 30px 0;\">" +
                "<div style=\"background-color: #F8F9FA; border: 2px dashed #1976D2; border-radius: 12px; padding: 20px 50px; display: inline-block;\">" +
                "<span style=\"font-size: 32px; font-weight: 700; color: #1976D2; letter-spacing: 8px; font-family: 'Courier New', monospace;\">" +
                events.code() +
                "</span>" +
                "</div>" +
                "</td>" +
                "</tr>" +
                "</table>" +

                "<p style=\"margin: 0 0 15px 0; color: #636E72; font-size: 15px; line-height: 1.6; text-align: center;\">" +
                "This verification code will expire in <strong style=\"color: #D32F2F;\">10 minutes</strong>." +
                "</p>" +

                "<hr style=\"border: none; border-top: 1px solid #EEEEEE; margin: 30px 0;\" />" +

                "<p style=\"margin: 0; color: #B2BEC3; font-size: 13px; line-height: 1.5; text-align: center;\">" +
                "If you did not request this code, please ignore this email or contact our support team immediately." +
                "</p>" +

                "</td>" +
                "</tr>" +

                "<tr>" +
                "<td style=\"background-color: #F1F2F6; padding: 20px; text-align: center; border-top: 1px solid #E0E0E0;\">" +
                "<p style=\"margin: 0; color: #95A5A6; font-size: 12px;\">&copy; 2026 NagarSetu. All rights reserved.</p>" +
                "</td>" +
                "</tr>" +

                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>" +

                "</body>" +
                "</html>";
    }

}
