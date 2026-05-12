package auth

import (
	"fmt"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v4"
)

type claims struct {
	UserID string `json:"uid"`
	jwt.RegisteredClaims
}

type confirmClaims struct {
	UserID string `json:"uid"`
	Type   string `json:"type"` // "email_confirm"
	jwt.RegisteredClaims
}

func GenerateAccessToken(userID string) (string, error) {
	ttl, err := time.ParseDuration(os.Getenv("JWT_ACCESS_TTL"))
	if err != nil {
		ttl = 15 * time.Minute
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims{
		UserID: userID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(ttl)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	})
	return token.SignedString(jwtSecret())
}

func ParseAccessToken(tokenStr string) (string, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &claims{}, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
		}
		return jwtSecret(), nil
	})
	if err != nil {
		return "", err
	}
	c, ok := token.Claims.(*claims)
	if !ok || !token.Valid {
		return "", fmt.Errorf("invalid token")
	}
	return c.UserID, nil
}

// GenerateConfirmToken создаёт JWT для подтверждения email (24 часа).
func GenerateConfirmToken(userID string) (string, error) {
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, confirmClaims{
		UserID: userID,
		Type:   "email_confirm",
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	})
	return token.SignedString(jwtSecret())
}

// ParseConfirmToken проверяет токен подтверждения и возвращает userID.
func ParseConfirmToken(tokenStr string) (string, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &confirmClaims{}, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
		}
		return jwtSecret(), nil
	})
	if err != nil {
		return "", err
	}
	c, ok := token.Claims.(*confirmClaims)
	if !ok || !token.Valid || c.Type != "email_confirm" {
		return "", fmt.Errorf("invalid confirm token")
	}
	return c.UserID, nil
}

// GenerateResetToken создаёт JWT для сброса пароля (1 час).
func GenerateResetToken(userID string) (string, error) {
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, confirmClaims{
		UserID: userID,
		Type:   "password_reset",
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	})
	return token.SignedString(jwtSecret())
}

// ParseResetToken проверяет токен сброса пароля и возвращает userID.
func ParseResetToken(tokenStr string) (string, error) {
	token, err := jwt.ParseWithClaims(tokenStr, &confirmClaims{}, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", t.Header["alg"])
		}
		return jwtSecret(), nil
	})
	if err != nil {
		return "", err
	}
	c, ok := token.Claims.(*confirmClaims)
	if !ok || !token.Valid || c.Type != "password_reset" {
		return "", fmt.Errorf("invalid reset token")
	}
	return c.UserID, nil
}

func jwtSecret() []byte {
	return []byte(os.Getenv("JWT_SECRET"))
}
