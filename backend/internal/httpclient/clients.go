package httpclient

import (
	"net"
	"net/http"
	"time"
)

// YandexOAuth creates an HTTP client for the Yandex ID OAuth and profile APIs.
// Yandex typically responds quickly; 10s total is generous.
func YandexOAuth() *http.Client {
	return &http.Client{
		Timeout: 10 * time.Second,
		Transport: &http.Transport{
			DialContext:           (&net.Dialer{Timeout: 3 * time.Second}).DialContext,
			TLSHandshakeTimeout:   3 * time.Second,
			ResponseHeaderTimeout: 5 * time.Second,
			MaxIdleConns:          10,
			IdleConnTimeout:       30 * time.Second,
		},
	}
}

// FCM creates an HTTP client for Firebase Cloud Messaging.
// In Russia FCM may route through CDN with added latency, hence 15s.
func FCM() *http.Client {
	return &http.Client{
		Timeout:   15 * time.Second,
		Transport: defaultTransport(),
	}
}

// RuStore creates an HTTP client for the RuStore Push API.
func RuStore() *http.Client {
	return &http.Client{
		Timeout:   10 * time.Second,
		Transport: defaultTransport(),
	}
}

func defaultTransport() *http.Transport {
	return &http.Transport{
		DialContext:           (&net.Dialer{Timeout: 3 * time.Second}).DialContext,
		TLSHandshakeTimeout:   5 * time.Second,
		ResponseHeaderTimeout: 8 * time.Second,
		MaxIdleConns:          20,
		IdleConnTimeout:       60 * time.Second,
	}
}
