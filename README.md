# Tài Liệu Prototype - Project Management MCP Server với Authentication

## 1. Tổng Quan 

### 1.1. Mô Tả
　Dự án Project Management MCP Server với Authentication là một MCP (Model Context Protocol) server được xây dựng trên Cloudflare Workers, cung cấp các chức năng quản lý dự án và công việc bằng việc giao tiếp với AI bằng ngôn ngữ tự nhiên (Cursor, Claude) với xác thực qua GitHub OAuth.

### 1.2. Kiến Trúc Hệ Thống
- **Platform**: Cloudflare Workers
- **Authentication**: GitHub OAuth 2.1
- **Storage**: Cloudflare KV (Key-Value Store)
- **State Management**: Durable Objects
- **Protocol**: Model Context Protocol (MCP)
- **Transport**: Streamable-HTTP và SSE (Server-Sent Events)

### 1.3. Công Nghệ Sử Dụng
- TypeScript
- @cloudflare/workers-oauth-provider
- @modelcontextprotocol/sdk
- Octokit (GitHub API)
- Zod (Schema validation)
- Hono (HTTP framework)

---

## 2. Đăng Nhập và Xác Thực

**Mục đích**: Người dùng đăng nhập vào hệ thống thông qua GitHub OAuth

**Luồng thực hiện**:
1. Client (MCP Client như Claude Desktop, Cursor) kết nối đến MCP Server
2. Server yêu cầu xác thực và redirect đến GitHub OAuth
3. Người dùng đăng nhập GitHub và cấp quyền
4. GitHub redirect về `/callback` với authorization code
5. Server trao đổi code lấy access token
6. Server lấy thông tin user từ GitHub API (login, name, email)
7. Server tạo OAuth token và lưu thông tin user vào props
8. Client nhận token và có thể sử dụng các tools

**Điều kiện**:
- User phải có tài khoản GitHub hợp lệ
- User phải nằm trong danh sách `ALLOWED_USERNAMES` để truy cập đầy đủ tính năng
- Client phải được approve trong lần đầu tiên

## 3. Kiến trúc

```
┌─────────────────┐
│  MCP Client     │
│ (Claude/Cursor) │
└────────┬────────┘
         │ HTTPS/SSE
         │ OAuth Flow
         ▼
┌─────────────────────────────────────┐
│   Cloudflare Workers                │
│   ┌───────────────────────────────┐ │
│   │  OAuth Provider               │ │
│   │  - /authorize                 │ │
│   │  - /callback                  │ │
│   │  - /token                     │ │
│   └───────────┬───────────────────┘ │
│               │                     │
│   ┌───────────▼───────────────────┐ │
│   │  MCP Server (MyMCP)           │ │
│   │  - /mcp (Streamable-HTTP)    │ │
│   │  - /sse (SSE - deprecated)   │ │
│   │  - Tools:                     │ │
│   │    • create_project           │ │
│   │    • list_projects            │ │
│   │    • get_projects             │ │
│   │    • create_todo              │ │
│   │    • update_todo              │ │
│   │    • delete_todo              │ │
│   │    • get_todo                 │ │
│   │    • list_todo                │ │
│   │    • delete_projects          │ │
│   └───────────┬───────────────────┘ │
└───────────────┼─────────────────────┘
                │
    ┌───────────┴───────────┐
    │                       │
    ▼                       ▼
┌──────────┐         ┌──────────────┐
│ GitHub   │         │ Cloudflare KV│
│ OAuth    │         │ - OAUTH_KV   │
│ API      │         │ - Project_   │
│          │         │   management │
└──────────┘         └──────────────┘
```

### Luồng Xác Thực OAuth

```
1. Client Request
   └─> GET /authorize?client_id=xxx&redirect_uri=yyy
       │
       ├─> Check if client approved
       │   ├─> Yes: Create OAuth state → Redirect to GitHub
       │   └─> No: Show approval dialog
       │
2. User Approval 
   └─> POST /authorize
       ├─> Validate CSRF token
       ├─> Add client to approved list
       ├─> Create OAuth state
       └─> Redirect to GitHub
           │
3. GitHub OAuth
   └─> GET https://github.com/login/oauth/authorize
       ├─> User logs in GitHub
       ├─> User grants permission
       └─> Redirect to /callback?code=xxx&state=yyy
           │
4. OAuth Callback
   └─> GET /callback?code=xxx&state=yyy
       ├─> Validate OAuth state (KV + Cookie)
       ├─> Exchange code for access token
       │   └─> POST https://github.com/login/oauth/access_token
       ├─> Get user info from GitHub
       │   └─> GET https://api.github.com/user
       ├─> Create OAuth token with user props
       └─> Redirect to client callback URL
           │
5. MCP Connection
   └─> Client uses token to connect
       ├─> POST /mcp (Streamable-HTTP)
       └─> Server validates token
           └─> MyMCP.init() with user props
               └─> Tools available based on user
```

### 3.3. Cấu Trúc Dữ Liệu trong KV

- **Key**: `oauth_state:{stateToken}`
- **Value**: `{ oauthReqInfo, expiresAt }`
- **Namespace**: `OAUTH_KV`
- **TTL**: Tự động expire sau một khoảng thời gian


### 3.4. API Endpoints

#### OAuth Endpoints
- `GET /authorize` - Bắt đầu OAuth flow, hiển thị approval dialog
- `POST /authorize` - Xử lý approval từ user
- `GET /callback` - Callback từ GitHub sau khi user authorize
- `POST /token` - Token endpoint (handled by OAuth Provider)
- `POST /register` - Client registration endpoint

#### MCP Endpoints
- `POST /mcp` - Streamable-HTTP protocol 
- `GET /sse` - Server-Sent Events protocol 

### 3.5. MCP Tools

#### Project Management Tools

1. **create_project** - Tạo dự án mới với tên và mô tả. Trả về thông tin dự án đã tạo bao gồm ID, ngày tạo và cập nhật.

2. **list_projects** - Liệt kê tất cả các dự án của user hiện tại. Trả về mảng các project objects.

3. **get_projects** - Lấy thông tin chi tiết của một dự án theo ID, bao gồm thông tin dự án và danh sách todos liên quan.

4. **delete_projects** - Xóa một dự án và tất cả todos thuộc dự án đó. Yêu cầu project_id để xác định dự án cần xóa.

#### Todo Management Tools

5. **create_todo** - Tạo todo mới trong một dự án. Yêu cầu project_id, title, description và optional priority (low/medium/high). Mặc định status là "in_progress".

6. **update_todo** - Cập nhật thông tin todo (title, description, status, priority). Yêu cầu todo_id và description (bắt buộc), các trường khác là optional.

7. **get_todo** - Lấy thông tin chi tiết của một todo theo ID. Trả về đầy đủ thông tin todo bao gồm projectId, status, priority và timestamps.

8. **list_todo** - Liệt kê todos trong một dự án. Có thể lọc theo trạng thái (pending/in_progress/done/all). Yêu cầu project_id.

9. **delete_todo** - Xóa một todo khỏi dự án. Yêu cầu todo_id để xác định todo cần xóa.

### 3.6. Tích Hợp với MCP Clients

#### 3.6.1. Claude Desktop
Cấu hình trong `claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "project-management": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "https://project-management-with-auth.ngvq03.workers.dev/sse"
      ]
    }
  }
}
```

#### 3.6.2. Cursor
cấu hình trong cursor setting (`mcp.json`)
```json
{
  "mcpServers": {
    "project-management": {
      "command": "mcp-remote",
      "args": [
        "https://project-management-with-auth.ngvq03.workers.dev/sse"
      ]
    }
  }
}
```

### 3.7 Bảo Mật

#### 3.7.1. Authentication & Authorization
- OAuth 2.1 flow với GitHub
- CSRF protection cho approval dialog
- Session binding với cookies
- State validation (KV + Cookie)
- Access control qua `ALLOWED_USERNAMES`

#### 3.7.2. Data Isolation
- Mỗi user có namespace riêng trong KV (prefix `user-{login}`)
- User chỉ có thể truy cập projects và todos của chính mình
- Keys được prefix với user login để tránh conflict

#### 3.7.3. Token Security
- OAuth tokens được mã hóa và lưu trữ an toàn
- Tokens có expiration time
- User context (props) được encrypt trong token

### 3.8. Environment Variables & Secrets

#### Required Secrets (set via `wrangler secret put`)
- `GITHUB_CLIENT_ID`: GitHub OAuth App Client ID
- `GITHUB_CLIENT_SECRET`: GitHub OAuth App Client Secret
- `COOKIE_ENCRYPTION_KEY`: Key để mã hóa cookies (random string, e.g., `openssl rand -hex 32`)

#### KV Namespaces (configured in wrangler.jsonc)
- `OAUTH_KV`: Lưu trữ OAuth state và session data
- `Project_management_store_with_Auth`: Lưu trữ projects và todos

### 3.9. Deployment

#### Production Deployment
```bash
# 1. Set secrets
wrangler secret put GITHUB_CLIENT_ID
wrangler secret put GITHUB_CLIENT_SECRET
wrangler secret put COOKIE_ENCRYPTION_KEY

# 2. Create KV namespaces (if not exists)
wrangler kv namespace create "OAUTH_KV"
wrangler kv namespace create "Project_management_store_with_Auth"

# 3. Update wrangler.jsonc with KV IDs

# 4. Deploy
wrangler deploy
```

## Tài Liệu Tham Khảo

- [Model Context Protocol Documentation](https://modelcontextprotocol.io/)
- [Cloudflare Workers Documentation](https://developers.cloudflare.com/workers/)
- [GitHub OAuth Documentation](https://docs.github.com/en/apps/oauth-apps)
- [MCP SDK Documentation](https://github.com/modelcontextprotocol/sdk)

---



https://www.youtube.com/embed/iadzYtX4ERU





