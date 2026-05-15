package main

import (
	"archive/zip"
	"io"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	root := "."
	outPath := filepath.Join("deploy", "ycfunc", "sources_unix.zip")

	dirs := []string{"internal", "migrations", "ycfunc"}
	files := []string{"go.mod", "go.sum", "sa_key.json", "handler.go"}

	out, err := os.Create(outPath)
	must(err)
	defer out.Close()

	w := zip.NewWriter(out)
	defer w.Close()

	for _, dir := range dirs {
		must(addDir(w, root, dir))
	}
	for _, f := range files {
		must(addFile(w, root, f))
	}
}

func addDir(w *zip.Writer, root, dir string) error {
	return filepath.Walk(filepath.Join(root, dir), func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return nil
		}
		rel, err := filepath.Rel(root, path)
		if err != nil {
			return err
		}
		return writeEntry(w, path, toUnix(rel))
	})
}

func addFile(w *zip.Writer, root, name string) error {
	return writeEntry(w, filepath.Join(root, name), name)
}

func writeEntry(w *zip.Writer, src, zipPath string) error {
	f, err := os.Open(src)
	if err != nil {
		return err
	}
	defer f.Close()

	entry, err := w.Create(zipPath)
	if err != nil {
		return err
	}
	_, err = io.Copy(entry, f)
	return err
}

func toUnix(p string) string {
	return strings.ReplaceAll(p, `\`, "/")
}

func must(err error) {
	if err != nil {
		panic(err)
	}
}
