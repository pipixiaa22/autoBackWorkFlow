# 音频任务失败信息与 SSE 前端接入

## 创建任务

`POST /api/v1/projects/{projectId}/audio-tasks` 的请求格式不变。ID 使用字符串传递，避免 JavaScript 数字精度损失：

```json
{
  "providerId": "900000000000000001",
  "modelId": "900000000000000101",
  "segmentIds": ["2102326328953987074"],
  "parameters": {}
}
```

创建接口返回任务快照，通常是 `PENDING`。取响应 `data.id` 后立即连接 SSE。后端在创建事务提交后启动生成，异步失败不会改变创建接口的 HTTP 状态。

## 订阅任务事件

`GET /api/v1/audio-tasks/{id}/events`

响应类型为 `text/event-stream`。连接建立后立即收到一份任务快照；之后任务或子任务状态变化时推送新快照。服务端每 15 秒发送 SSE 注释作为保活。

| 事件名 | 含义 |
| --- | --- |
| `task` | 非终态快照，继续监听 |
| `complete` | 任务进入 `SUCCEEDED`、`PARTIAL_SUCCESS`、`FAILED` 或 `CANCELLED`，服务端随后关闭连接 |

`data` 为 JSON 对象，结构与 `GET /api/v1/audio-tasks/{id}` 的 `data` 一致：

```json
{
  "task": {
    "id": "123",
    "status": "FAILED",
    "totalCount": 1,
    "successCount": 0,
    "failedCount": 1,
    "progressPercent": 100,
    "errorCode": "PROVIDER_REQUEST_REJECTED",
    "errorMessage": "SeedAudio 生成失败（code=123）"
  },
  "items": [
    {
      "id": "456",
      "segmentId": "2102326328953987074",
      "status": "FAILED",
      "errorCode": "PROVIDER_REQUEST_REJECTED",
      "errorMessage": "SeedAudio 生成失败（code=123）"
    }
  ]
}
```

示例中省略了其他任务和子任务字段。实际供应商错误码和文案以响应为准。`task.errorMessage` 会汇总首个失败子任务的原因；逐条失败原因在 `items[].errorMessage`。内部异常会返回可展示的摘要，完整堆栈写入服务端日志。

## 前端改动示例

```js
const terminal = new Set(['SUCCEEDED', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED']);

function watchAudioTask(taskId, onSnapshot, onDisconnected) {
  const source = new EventSource(`/api/v1/audio-tasks/${encodeURIComponent(taskId)}/events`);

  function handle(event) {
    const snapshot = JSON.parse(event.data);
    onSnapshot(snapshot);
    if (terminal.has(snapshot.task.status)) source.close();
  }

  source.addEventListener('task', handle);
  source.addEventListener('complete', handle);
  source.onerror = () => {
    // EventSource 会自动重连；重新连接后服务端会立即发送最新快照。
    onDisconnected?.();
  };
  return () => source.close(); // 页面离开或切换任务时调用
}
```

用 SSE 快照更新进度、每条分段状态和错误提示，停止原先每 1–2 秒请求 `GET /api/v1/audio-tasks/{id}` 的轮询。进入已有任务页面、或需要在断线时主动恢复状态，仍可调用该 GET 接口；它现在也会返回任务级和子任务级错误字段。

`EventSource` 不能设置自定义请求头；当前接口不要求额外请求头。如果前端通过开发代理访问后端，应确认代理允许 `text/event-stream` 持续传输且不缓冲响应。

## 取消和重试

原有 `POST /api/v1/audio-tasks/{id}/cancel` 与 `POST /api/v1/audio-items/{id}/retry` 保持不变。重试会把任务重新置为 `PENDING`，清除旧任务错误并再次推送状态。若上一次 SSE 已因任务终态关闭，重试后需为该任务重新建立连接。
