import request from './request'

export const listUserAuditLogs = (targetUserId) =>
  request.get('/users/audits', {
    params: targetUserId ? { targetUserId } : undefined,
  })
