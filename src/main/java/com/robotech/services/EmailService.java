package com.robotech.services;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.from.email}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // ===========================
    // MÉTODO PRIVADO CENTRAL
    // ===========================
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("✅ Email enviado exitosamente a: " + to + " | ID: " + response.getId());

        } catch (ResendException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al enviar email via Resend: " + e.getMessage(), e);
        }
    }

    // ===========================
    // ENVÍO DE CREDENCIALES
    // ===========================
    public void sendCredentialsEmail(String toEmail, String userName, String temporalPassword, List<String> roles) {
        System.out.println("📧 Intentando enviar email a: " + toEmail);
        System.out.println("📧 Desde: " + fromEmail);
        String htmlContent = buildCredentialsEmailTemplate(userName, toEmail, temporalPassword, roles);
        sendEmail(toEmail, "🔑 Bienvenido a RoboTech - Tus Credenciales de Acceso", htmlContent);
        System.out.println("✅ Email enviado exitosamente a: " + toEmail);
    }

    // ===========================
    // ENVÍO RECUPERACIÓN CONTRASEÑA
    // ===========================
    public void sendPasswordResetEmail(String toEmail, String token, String userName) {
        System.out.println("📧 Intentando enviar email de recuperación a: " + toEmail);
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String htmlContent = buildPasswordResetEmailTemplate(userName, resetLink);
        sendEmail(toEmail, "🔐 Recuperación de Contraseña - RoboTech", htmlContent);
        System.out.println("✅ Email de recuperación enviado a: " + toEmail);
    }

    // ===========================
    // ENVÍO RESTABLECIMIENTO DE EMAIL
    // ===========================
    public void sendEmailResetCredentials(String toEmail, String userName, String nuevoEmail,
            String nuevaPassword, String emailAnterior) {
        System.out.println("📧 Intentando enviar email de restablecimiento a: " + toEmail);
        String htmlContent = buildEmailResetTemplate(userName, nuevoEmail, nuevaPassword, emailAnterior);
        sendEmail(toEmail, "🔄 Restablecimiento de Email - RoboTech", htmlContent);
        System.out.println("✅ Email de restablecimiento enviado a: " + toEmail);
    }

    // ===========================
    // NOTIFICACIÓN DESHABILITACIÓN CLUB
    // ===========================
    public void sendClubDeshabilitacionNotification(String toEmail, String userName,
            String clubNombre, String motivo,
            LocalDateTime fechaLimite, Long deshabilitacionId) {
        System.out.println("📧 Enviando notificación de deshabilitación a: " + toEmail);
        String htmlContent = buildClubDeshabilitacionTemplate(userName, clubNombre, motivo, fechaLimite, deshabilitacionId);
        sendEmail(toEmail, "🚨 URGENTE: Tu club será deshabilitado - RoboTech", htmlContent);
        System.out.println("✅ Email de deshabilitación enviado a: " + toEmail);
    }

    // ===========================
    // NOTIFICACIÓN DEGRADACIÓN
    // ===========================
    public void sendDegradacionNotification(String toEmail, String userName, String clubNombre) {
        System.out.println("📧 Enviando notificación de degradación a: " + toEmail);
        String htmlContent = buildDegradacionTemplate(userName, clubNombre);
        sendEmail(toEmail, "⚠️ Cambio en tu cuenta - RoboTech", htmlContent);
        System.out.println("✅ Email de degradación enviado a: " + toEmail);
    }

    // ===========================
    // TEMPLATE CREDENCIALES
    // ===========================
    private String buildCredentialsEmailTemplate(String userName, String email, String temporalPassword,
            List<String> roles) {

        String rolesFormatted = String.join(", ", roles.stream()
                .map(r -> r.replace("ROLE_", ""))
                .toList());

        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 30px auto; background-color: #ffffff;
                                     border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                  color: white; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; }
                        .content { padding: 40px 30px; }
                        .content h2 { color: #333; margin-top: 0; }
                        .content p { color: #666; line-height: 1.6; font-size: 16px; }
                        .credentials-box { background-color: #f8f9fa; padding: 20px; border-radius: 8px;
                                           margin: 20px 0; border-left: 4px solid #667eea; }
                        .value { color: #667eea; font-size: 18px; font-weight: bold; font-family: 'Courier New', monospace; }
                        .button { display: inline-block; padding: 15px 40px; margin: 20px 0;
                                  background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                  color: white; text-decoration: none; border-radius: 5px; font-weight: bold; }
                        .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .footer { background-color: #f8f9fa; padding: 20px; text-align: center;
                                  color: #6c757d; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🤖 RoboTech</h1>
                            <p>Sistema de Gestión de Torneos de Robótica</p>
                        </div>

                        <div class="content">
                            <h2>¡Bienvenido a RoboTech, {userName}!</h2>

                            <p>El administrador ha creado una cuenta para ti. Aquí están tus credenciales:</p>

                            <div class="credentials-box">
                                <strong>📧 Email:</strong>
                                <div class="value">{email}</div>
                            </div>

                            <div class="credentials-box">
                                <strong>🔑 Contraseña Temporal:</strong>
                                <div class="value">{password}</div>
                            </div>

                            <div class="credentials-box">
                                <strong>👤 Roles Asignados:</strong>
                                <div class="value">{roles}</div>
                            </div>

                            <center>
                                <a href="{frontendUrl}/login" class="button">🚀 Iniciar Sesión</a>
                            </center>

                            <div class="warning">
                                <strong>⚠️ Importante:</strong>
                                <ul>
                                    <li>Contraseña temporal generada automáticamente</li>
                                    <li>Debes cambiarla al iniciar sesión</li>
                                    <li>No compartas estas credenciales</li>
                                </ul>
                            </div>

                            <p>Saludos,<br><strong>Equipo RoboTech</strong></p>
                        </div>

                        <div class="footer">
                            <p>Este es un correo automático. No responder.</p>
                            <p>&copy; 2024 RoboTech. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """;

        return template
                .replace("{userName}", userName)
                .replace("{email}", email)
                .replace("{password}", temporalPassword)
                .replace("{roles}", rolesFormatted)
                .replace("{frontendUrl}", frontendUrl);
    }

    // ===========================
    // TEMPLATE RECUPERAR CONTRASEÑA
    // ===========================
    private String buildPasswordResetEmailTemplate(String userName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            background-color: #f4f4f4;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 30px auto;
                            background-color: #ffffff;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content h2 {
                            color: #333;
                            margin-top: 0;
                        }
                        .content p {
                            color: #666;
                            line-height: 1.6;
                            font-size: 16px;
                        }
                        .button {
                            display: inline-block;
                            padding: 15px 40px;
                            margin: 20px 0;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            text-decoration: none;
                            border-radius: 5px;
                            font-weight: bold;
                            text-align: center;
                        }
                        .button:hover {
                            opacity: 0.9;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px;
                            text-align: center;
                            color: #6c757d;
                            font-size: 14px;
                        }
                        .link-box {
                            background-color: #f8f9fa;
                            padding: 15px;
                            border-radius: 5px;
                            word-break: break-all;
                            margin: 10px 0;
                            font-size: 12px;
                            color: #666;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🤖 RoboTech</h1>
                            <p>Sistema de Gestión de Torneos de Robótica</p>
                        </div>

                        <div class="content">
                            <h2>Hola %s,</h2>

                            <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.</p>

                            <p>Si fuiste tú quien realizó esta solicitud, haz clic en el siguiente botón para crear una nueva contraseña:</p>

                            <center>
                                <a href="%s" class="button">🔐 Restablecer Contraseña</a>
                            </center>

                            <div class="warning">
                                <strong>⚠️ Importante:</strong> Este enlace es válido por 24 horas y solo puede usarse una vez.
                            </div>

                            <p>Si el botón no funciona, copia y pega el siguiente enlace en tu navegador:</p>

                            <div class="link-box">
                                %s
                            </div>

                            <p><strong>¿No solicitaste esto?</strong><br>
                            Si no solicitaste restablecer tu contraseña, puedes ignorar este correo de forma segura. Tu contraseña actual seguirá siendo válida.</p>

                            <p>Para cualquier problema o consulta, contáctanos respondiendo a este correo.</p>

                            <p>Saludos,<br>
                            <strong>El equipo de RoboTech</strong></p>
                        </div>

                        <div class="footer">
                            <p>Este es un correo automático, por favor no respondas directamente.</p>
                            <p>&copy; 2024 RoboTech. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(userName, resetLink, resetLink);
    }

    // ===========================
    // TEMPLATE RESTABLECIMIENTO EMAIL
    // ===========================
    private String buildEmailResetTemplate(String userName, String nuevoEmail, String nuevaPassword,
            String emailAnterior) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            background-color: #f4f4f4;
                            margin: 0;
                            padding: 0;
                        }
                        .container {
                            max-width: 600px;
                            margin: 30px auto;
                            background-color: #ffffff;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content h2 {
                            color: #333;
                            margin-top: 0;
                        }
                        .content p {
                            color: #666;
                            line-height: 1.6;
                            font-size: 16px;
                        }
                        .credentials-box {
                            background-color: #f8f9fa;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 20px 0;
                            border-left: 4px solid #667eea;
                        }
                        .value {
                            color: #667eea;
                            font-size: 18px;
                            font-weight: bold;
                            font-family: 'Courier New', monospace;
                        }
                        .button {
                            display: inline-block;
                            padding: 15px 40px;
                            margin: 20px 0;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            text-decoration: none;
                            border-radius: 5px;
                            font-weight: bold;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .info-box {
                            background-color: #e7f3ff;
                            border-left: 4px solid #2196F3;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px;
                            text-align: center;
                            color: #6c757d;
                            font-size: 14px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🤖 RoboTech</h1>
                            <p>Sistema de Gestión de Torneos de Robótica</p>
                        </div>

                        <div class="content">
                            <h2>Hola %s,</h2>

                            <p>El administrador ha restablecido tu correo electrónico y contraseña.</p>

                            <div class="info-box">
                                <strong>📧 Email Anterior:</strong>
                                <div style="font-family: 'Courier New', monospace; margin-top: 5px;">%s</div>
                            </div>

                            <div class="credentials-box">
                                <strong>📧 Nuevo Email:</strong>
                                <div class="value">%s</div>
                            </div>

                            <div class="credentials-box">
                                <strong>🔑 Nueva Contraseña Temporal:</strong>
                                <div class="value">%s</div>
                            </div>

                            <center>
                                <a href="{frontendUrl}/login" class="button">🚀 Iniciar Sesión</a>
                            </center>

                            <div class="warning">
                                <strong>⚠️ Importante:</strong>
                                <ul>
                                    <li>Tu email ha sido cambiado exitosamente</li>
                                    <li>Usa el nuevo email para iniciar sesión</li>
                                    <li>Cambia tu contraseña temporal lo antes posible</li>
                                    <li>Si no solicitaste este cambio, contacta al administrador</li>
                                </ul>
                            </div>

                            <p>Saludos,<br>
                            <strong>El equipo de RoboTech</strong></p>
                        </div>

                        <div class="footer">
                            <p>Este es un correo automático, por favor no respondas directamente.</p>
                            <p>&copy; 2024 RoboTech. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, emailAnterior, nuevoEmail, nuevaPassword)
                .replace("{frontendUrl}", frontendUrl);
    }

    // ===========================
    // TEMPLATE DESHABILITACIÓN CLUB
    // ===========================
    private String buildClubDeshabilitacionTemplate(String userName, String clubNombre,
            String motivo, LocalDateTime fechaLimite,
            Long deshabilitacionId) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormateada = fechaLimite.format(formatter);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 30px auto; background-color: #ffffff;
                                     border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #e74c3c 0%%, #c0392b 100%%);
                                  color: white; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; }
                        .content { padding: 40px 30px; }
                        .content h2 { color: #c0392b; margin-top: 0; }
                        .content p { color: #666; line-height: 1.6; font-size: 16px; }
                        .warning-box { background-color: #fff3cd; border-left: 4px solid #ffc107;
                                       padding: 20px; margin: 20px 0; border-radius: 5px; }
                        .info-box { background-color: #e7f3ff; border-left: 4px solid #2196F3;
                                    padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .danger-box { background-color: #ffe0e0; border-left: 4px solid #e74c3c;
                                      padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .button { display: inline-block; padding: 15px 40px; margin: 20px 0;
                                  background: linear-gradient(135deg, #e74c3c 0%%, #c0392b 100%%);
                                  color: white; text-decoration: none; border-radius: 5px; font-weight: bold; }
                        .footer { background-color: #f8f9fa; padding: 20px; text-align: center;
                                  color: #6c757d; font-size: 14px; }
                        .urgent { color: #e74c3c; font-weight: bold; font-size: 18px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🚨 ACCIÓN REQUERIDA</h1>
                            <p>Notificación Importante</p>
                        </div>

                        <div class="content">
                            <h2>Hola %s,</h2>

                            <div class="danger-box">
                                <p class="urgent">⚠️ Tu club "%s" será deshabilitado</p>
                            </div>

                            <p><strong>Motivo:</strong></p>
                            <div class="info-box">
                                %s
                            </div>

                            <div class="warning-box">
                                <strong>📅 Fecha límite para actuar:</strong>
                                <p style="font-size: 20px; margin: 10px 0;"><strong>%s</strong></p>
                            </div>

                            <h3>🔄 Opciones disponibles:</h3>
                            <ol style="line-height: 1.8;">
                                <li><strong>Solicitar transferencia</strong> a otro club activo</li>
                                <li><strong>Esperar</strong> que el administrador te asigne a otro club</li>
                                <li><strong>No hacer nada</strong> (perderás el rol de competidor)</li>
                            </ol>

                            <div class="danger-box">
                                <strong>⚠️ IMPORTANTE:</strong>
                                <ul>
                                    <li>Si no actúas antes de la fecha límite, perderás tu rol de COMPETIDOR</li>
                                    <li>Pasarás a ser un usuario básico (sin competir)</li>
                                    <li>Tus robots en estado PENDIENTE serán rechazados</li>
                                    <li>Tus robots APROBADOS se mantendrán en tu historial</li>
                                </ul>
                            </div>

                            <center>
                                <a href="%s/transferencias" class="button">🔄 Solicitar Transferencia</a>
                            </center>

                            <p style="margin-top: 30px;">Si tienes dudas, contacta con el administrador.</p>

                            <p>Saludos,<br>
                            <strong>El equipo de RoboTech</strong></p>
                        </div>

                        <div class="footer">
                            <p>Este es un correo automático. Por favor actúa antes de la fecha límite.</p>
                            <p>&copy; 2024 RoboTech. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, clubNombre, motivo, fechaFormateada, frontendUrl);
    }

    // ===========================
    // TEMPLATE DEGRADACIÓN
    // ===========================
    private String buildDegradacionTemplate(String userName, String clubNombre) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 30px auto; background-color: #ffffff;
                                     border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }
                        .header { background: linear-gradient(135deg, #95a5a6 0%%, #7f8c8d 100%%);
                                  color: white; padding: 30px; text-align: center; }
                        .content { padding: 40px 30px; }
                        .info-box { background-color: #e7f3ff; border-left: 4px solid #2196F3;
                                    padding: 15px; margin: 20px 0; border-radius: 4px; }
                        .button { display: inline-block; padding: 15px 40px; margin: 20px 0;
                                  background: linear-gradient(135deg, #3498db 0%%, #2980b9 100%%);
                                  color: white; text-decoration: none; border-radius: 5px; }
                        .footer { background-color: #f8f9fa; padding: 20px; text-align: center;
                                  color: #6c757d; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>ℹ️ Cambio en tu cuenta</h1>
                        </div>

                        <div class="content">
                            <h2>Hola %s,</h2>

                            <p>Te informamos que debido a la deshabilitación del club "%s", tu cuenta ha sido actualizada.</p>

                            <div class="info-box">
                                <strong>Cambios realizados:</strong>
                                <ul>
                                    <li>✅ Sigues siendo usuario registrado</li>
                                    <li>⬇️ Se quitó tu rol de COMPETIDOR</li>
                                    <li>🏢 Ya no perteneces a ningún club</li>
                                    <li>🤖 Tus robots PENDIENTES fueron rechazados</li>
                                    <li>📋 Tus robots APROBADOS se mantienen en tu historial</li>
                                </ul>
                            </div>

                            <h3>🔄 ¿Qué puedes hacer ahora?</h3>
                            <p>Para volver a competir:</p>
                            <ol>
                                <li>Busca un club activo que te acepte</li>
                                <li>Solicita unirte a ese club</li>
                                <li>El club owner te revisará y aprobará</li>
                                <li>Recuperarás tu rol de COMPETIDOR</li>
                            </ol>

                            <center>
                                <a href="%s/clubs" class="button">🔍 Ver Clubs Disponibles</a>
                            </center>

                            <p>Saludos,<br><strong>El equipo de RoboTech</strong></p>
                        </div>

                        <div class="footer">
                            <p>&copy; 2024 RoboTech. Todos los derechos reservados.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(userName, clubNombre, frontendUrl);
    }
}