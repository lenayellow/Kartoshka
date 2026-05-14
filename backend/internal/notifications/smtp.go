package notifications

import (
	"crypto/tls"
	"fmt"
	"net"
	"net/smtp"
	"os"
	"time"
)

func SendResetEmail(to, name, resetURL string) error {
	body := fmt.Sprintf(`<html><body>
<p>Привет, %s!</p>
<p>Вы запросили сброс пароля в Супер Списках. Нажмите на кнопку ниже:</p>
<p><a href="%s" style="background:#4CAF50;color:white;padding:12px 24px;text-decoration:none;border-radius:6px">Сбросить пароль</a></p>
<p>Ссылка действительна 1 час.</p>
<p>Если вы не запрашивали сброс — просто проигнорируйте это письмо.</p>
</body></html>`, name, resetURL)

	return SendEmail(to, "Сброс пароля — Супер Списки", body)
}

func SendConfirmEmail(to, name, confirmURL string) error {
	body := fmt.Sprintf(`<html><body>
<p>Привет, %s!</p>
<p>Подтвердите ваш email, нажав на кнопку ниже:</p>
<p><a href="%s" style="background:#4CAF50;color:white;padding:12px 24px;text-decoration:none;border-radius:6px">Подтвердить email</a></p>
<p>Ссылка действительна 24 часа.</p>
<p>Если вы не регистрировались в Супер Списках — просто проигнорируйте это письмо.</p>
</body></html>`, name, confirmURL)

	return SendEmail(to, "Подтверждение email — Супер Списки", body)
}

func SendEmail(to, subject, htmlBody string) error {
	host := os.Getenv("SMTP_HOST")
	port := os.Getenv("SMTP_PORT")
	user := os.Getenv("SMTP_USER")
	pass := os.Getenv("SMTP_PASS")

	if host == "" || port == "" || user == "" || pass == "" {
		return fmt.Errorf("SMTP не настроен (SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS)")
	}

	// Порт 465 — прямое TLS-соединение. net.DialTimeout даёт явный dial timeout;
	// SetDeadline покрывает TLS-handshake и всю SMTP-сессию.
	rawConn, err := net.DialTimeout("tcp", net.JoinHostPort(host, port), 10*time.Second)
	if err != nil {
		return fmt.Errorf("smtp dial: %w", err)
	}
	rawConn.SetDeadline(time.Now().Add(30 * time.Second))
	tlsConn := tls.Client(rawConn, &tls.Config{ServerName: host})

	client, err := smtp.NewClient(tlsConn, host)
	if err != nil {
		return fmt.Errorf("smtp client: %w", err)
	}
	defer client.Close()

	if err := client.Auth(smtp.PlainAuth("", user, pass, host)); err != nil {
		return fmt.Errorf("smtp auth: %w", err)
	}
	if err := client.Mail(user); err != nil {
		return fmt.Errorf("smtp from: %w", err)
	}
	if err := client.Rcpt(to); err != nil {
		return fmt.Errorf("smtp rcpt: %w", err)
	}

	wc, err := client.Data()
	if err != nil {
		return fmt.Errorf("smtp data: %w", err)
	}

	msg := "From: " + user + "\r\n" +
		"To: " + to + "\r\n" +
		"Subject: " + subject + "\r\n" +
		"MIME-Version: 1.0\r\n" +
		"Content-Type: text/html; charset=utf-8\r\n\r\n" +
		htmlBody

	if _, err = fmt.Fprint(wc, msg); err != nil {
		return fmt.Errorf("smtp write: %w", err)
	}
	return wc.Close()
}
