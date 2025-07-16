# 🧼 Sanitizr

**Sanitizr** is a powerful Android utility app for cleaning metadata and timestamps from various file types including images, videos, documents, PDFs, archives, and ebooks.

> ✅ Your files, your privacy. Sanitizr helps you remove hidden data from files before sharing them.

---

## 📦 Features

- 🔍 **Scan folders** like Downloads, Pictures, and Documents
- 🖼️ **Image metadata** removal (EXIF: GPS, camera info, etc.)
- 📹 **Video/audio stream sanitization** via FFmpeg (removes tags)
- 📄 **PDF metadata** stripping
- 🧾 **Document metadata** cleanup (Word, Excel, PowerPoint)
- 📚 **Ebook sanitization** (EPUB .opf metadata)
- 🗜️ **Archive support**  
  - ZIP (.zip)  
  - TAR (.tar)  
  - GZ (.gz)  
  - BZ2 (.bz2)  
  - XZ (.xz)  
- ⚙️ Simple UI for selecting and sanitizing files in bulk

---

## 🛠️ How It Works

Sanitizr parses file types using known extensions and applies file-type-specific cleaning:

- **Images**: Clears EXIF tags using `ExifInterface`
- **PDFs**: Uses `OpenPDF` to remove PDF info
- **Office Docs**: `Apache POI` strips metadata
- **Videos/Audio**: Leverages `FFmpegKit` to drop metadata
- **EPUBs**: Parses and clears `<metadata>` from OPF
- **Archives**: Extract → sanitize → recompress using `Commons Compress`

---

## 📁 Supported File Types

| Type       | Extensions                                      |
|------------|--------------------------------------------------|
| Images     | jpg, jpeg, png, gif, webp, bmp, tiff, heic       |
| Videos     | mp4, mov, mkv, avi, webm, 3gp                    |
| Audio      | mp3, flac, wav, ogg, m4a, wma                    |
| PDFs       | pdf                                              |
| Docs       | doc, docx, pptx, xlsx, odt, rtf, csv, txt        |
| Ebooks     | epub, mobi, azw3, fb2                            |
| Archives   | zip, tar, gz, bz2, xz                            |

---

## 🧱 Built With

- [AndroidX](https://developer.android.com/jetpack/androidx)
- [FFmpegKit](https://github.com/arthenica/ffmpeg-kit)
- [OpenPDF](https://github.com/LibrePDF/OpenPDF)
- [Apache POI](https://poi.apache.org/)
- [Commons Compress](https://commons.apache.org/proper/commons-compress/)
- [XZ for Java](https://tukaani.org/xz/java.html)

---

## 🧪 Dev Features

- Modular sanitization functions (`sanitizeImage()`, `sanitizePdf()`, etc.)
- Archive-safe operations with temp files
- Non-destructive fallback if sanitization fails
- Scans `/Download`, `/Documents`, `/Pictures` by default

---

## 📷 UI Preview

> 🟧 **Modern clean UI**  
> 🟢 File list, selection controls, and progress bar  
> 🔧 Easy to customize layout with ConstraintLayout

---

## 🔒 Privacy First

Sanitizr works **offline**, never uploads files, and does **not** retain data after processing.

---



---

## 🚀 Getting Started (Dev)

1. Clone repo  
2. Open in Android Studio  
3. Run on device (or emulator with file access)

---

## 📌 License

MIT License — use, modify, and improve freely.

---

## 💡 Ideas for Next Version

- 📱 Share sanitized files directly
- ☁️ Add support for cloud files (MEGA)
- 🔍 Deep scan for nested archives or attachments
- 👁️ Preview metadata before and after

---

## ✨ Screenshots (Add Later)

> Want help creating a badge-style preview or UI mockup? Just ask.

---

> Made with ❤️  by: aMiscreant to help people control their data.
