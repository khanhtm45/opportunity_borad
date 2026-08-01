# Hermes Agent (Windows) + Cursor Skills

Full **Hermes Agent runtime** đã sẵn sàng trên Windows native, kèm Cursor Agent Skills từ 3 repo.

---

## Hermes Agent runtime — trạng thái

| Mục | Giá trị |
|-----|---------|
| Version | **v0.19.1** (2026.7.30) |
| Install | `%LOCALAPPDATA%\hermes\hermes-agent` |
| Data / config | `%LOCALAPPDATA%\hermes` (`HERMES_HOME`) |
| Python | 3.11.15 (venv) |
| Terminal backend | `local` |
| Bundled skills | **61 enabled** (đã seed) |
| `agent-browser` | OK |
| Git Bash | `C:\Program Files\Git\bin\bash.exe` |

`hermes` đã nằm trên **User PATH**. Mở **terminal mới** rồi chạy:

```powershell
hermes version
hermes doctor
hermes skills list
```

Hoặc trong repo này:

```powershell
. .\scripts\hermes-env.ps1
# hoặc
.\scripts\start-hermes.cmd
```

---

## Bước còn lại để chat được (bắt buộc)

Runtime đã chạy; còn thiếu **API key / provider**. Trong PowerShell **interactive** (Windows Terminal / Cursor terminal):

```powershell
# Cách 1 — wizard đầy đủ
hermes setup

# Cách 2 — chọn model/provider
hermes model

# Cách 3 — Nous Portal (OAuth, nhiều model)
hermes setup --portal

# Cách 4 — tự gắn key
notepad $env:LOCALAPPDATA\hermes\.env
# Thêm ví dụ:
# OPENROUTER_API_KEY=sk-or-...
# rồi:
hermes model
```

Sau khi có key:

```powershell
hermes                  # chat interactive
hermes -z "ping"        # one-shot
hermes doctor           # chỉ còn warning tùy chọn là OK
```

---

## Lệnh thường dùng

```powershell
hermes                  # chat
hermes model            # đổi provider/model
hermes tools            # bật/tắt tools
hermes skills list      # skill đã seed
hermes skills config    # enable/disable skill
hermes gateway          # Telegram/Discord/Slack…
hermes update           # cập nhật runtime
hermes desktop          # build/chạy Desktop app (tùy chọn)
hermes doctor --fix     # tự sửa cấu hình cơ bản
```

Cài lại / repair installer chính thức:

```powershell
iex (irm https://hermes-agent.nousresearch.com/install.ps1)
```

---

## Cursor Skills (đã cài global)

Đường dẫn: `C:\Users\truon\.agents\skills\` — **108 skills**

| Nguồn | Số | Dùng trong Cursor Agent |
|-------|----|-------------------------|
| mattpocock/skills | 41 | `/grill-with-docs`, `/tdd`, `/implement`… |
| huashu-design | 1 | prototype HTML / PPT / animation |
| hermes-agent skills | 66 | đã copy sang Cursor; Hermes runtime dùng bản seed riêng |

Trong Cursor Agent: gõ `/` → chọn skill. Lần đầu Matt: `/setup-matt-pocock-skills`.

---

## Cấu trúc Windows quan trọng

```
%LOCALAPPDATA%\hermes\
  .env                 ← API keys
  config.yaml          ← model, terminal, …
  skills\              ← 61+ bundled skills (đã seed)
  hermes-agent\        ← source + venv
    venv\Scripts\hermes.exe
  bin\uv.exe
```

Env đã set (User):

- `HERMES_HOME` = `%LOCALAPPDATA%\hermes`
- `HERMES_GIT_BASH_PATH` = Git Bash
- PATH gồm `...\hermes-agent\venv\Scripts` và `...\hermes\bin`

---

## Troubleshooting nhanh

| Lỗi | Cách xử lý |
|-----|------------|
| `hermes: command not found` | Mở terminal mới, hoặc `. .\scripts\hermes-env.ps1` |
| `No inference provider configured` | `hermes setup` / `hermes model` / thêm key vào `.env` |
| Antivirus quarantine `uv.exe` | Whitelist `%LOCALAPPDATA%\hermes\bin` |
| Skills list trống | `hermes skills opt-in --sync` |
| npm engine warning khi cài browser tools | `npm install --engine-strict=false` trong `hermes-agent` |

---

## File hỗ trợ trong repo

- `scripts/hermes-env.ps1` — load Hermes vào session hiện tại  
- `scripts/start-hermes.cmd` — chạy `hermes` từ Explorer/CMD  
- `_repos/` — clone tham khảo (đã ignore)
