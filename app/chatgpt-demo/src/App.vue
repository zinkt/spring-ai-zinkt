<template>
  <div class="airline-assistant">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="logo-section">
          <div class="logo">✈️</div>
          <h1 class="title">乐山航空助手</h1>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-number">{{ tableData.length }}</span>
            <span class="stat-label">总预订</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ confirmedBookings }}</span>
            <span class="stat-label">已确认</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ completedBookings }}</span>
            <span class="stat-label">已完成</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">{{ cancelledBookings }}</span>
            <span class="stat-label">已取消</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <el-row :gutter="20" class="content-row">
        <!-- 左侧预订管理区域 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16" class="bookings-section">
          <div class="section-card">
            <div class="section-header">
              <h2 class="section-title">我的预订</h2>
              <div class="booking-filters">
                <el-input
                    v-model="searchText"
                    placeholder="搜索预订号或乘客姓名"
                    size="small"
                    clearable
                    class="search-input"
                    :prefix-icon="Search"
                />
                <el-select v-model="statusFilter" placeholder="筛选状态" size="small" style="width: 120px;">
                  <el-option label="全部" value="all" />
                  <el-option label="已确认" value="CONFIRMED" />
                  <el-option label="已完成" value="COMPLETED" />
                  <el-option label="已取消" value="CANCELLED" />
                </el-select>
              </div>
            </div>

            <!-- 预订统计卡片 - 移到上方 -->
            <div class="booking-stats-cards">
              <div class="stats-card">
                <div class="stats-icon success">✓</div>
                <div class="stats-content">
                  <div class="stats-number">{{ confirmedBookings }}</div>
                  <div class="stats-label">已确认预订</div>
                </div>
              </div>
              <div class="stats-card">
                <div class="stats-icon completed">✅</div>
                <div class="stats-content">
                  <div class="stats-number">{{ completedBookings }}</div>
                  <div class="stats-label">已完成预订</div>
                </div>
              </div>
              <div class="stats-card">
                <div class="stats-icon cancelled">❌</div>
                <div class="stats-content">
                  <div class="stats-number">{{ cancelledBookings }}</div>
                  <div class="stats-label">已取消预订</div>
                </div>
              </div>
              <div class="stats-card">
                <div class="stats-icon info">📊</div>
                <div class="stats-content">
                  <div class="stats-number">{{ totalFlights }}</div>
                  <div class="stats-label">总航班数</div>
                </div>
              </div>
            </div>

            <div class="table-container">
              <el-table
                  :data="filteredTableData"
                  class="booking-table"
                  size="small"
                  :header-cell-style="{ backgroundColor: '#f8fafc', color: '#374151', fontWeight: '600', fontSize: '13px' }"
                  :row-style="{ backgroundColor: '#ffffff' }"
                  height="100%"
              >
                <el-table-column prop="bookingNumber" label="预订号" :width="isMobile ? 120 : 140">
                  <template #default="scope">
                    <div class="booking-number">{{ scope.row.bookingNumber }}</div>
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="乘客姓名" :width="isMobile ? 100 : 120" />
                <el-table-column prop="date" label="出行日期" :width="isMobile ? 100 : 120">
                  <template #default="scope">
                    <div class="date-cell">{{ scope.row.date }}</div>
                  </template>
                </el-table-column>
                <el-table-column prop="from" label="出发地" :width="isMobile ? 80 : 100">
                  <template #default="scope">
                    <div class="location-cell">{{ scope.row.from }}</div>
                  </template>
                </el-table-column>
                <el-table-column prop="to" label="目的地" :width="isMobile ? 80 : 100">
                  <template #default="scope">
                    <div class="location-cell">{{ scope.row.to }}</div>
                  </template>
                </el-table-column>
                <el-table-column prop="bookingStatus" label="状态" :width="isMobile ? 80 : 100">
                  <template #default="scope">
                    <el-tag
                        :type="getStatusType(scope.row.bookingStatus)"
                        class="status-tag"
                        effect="light"
                        size="small"
                    >
                      {{ getStatusText(scope.row.bookingStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="bookingClass" label="舱位等级" :width="isMobile ? 80 : 100" v-if="!isMobile">
                  <template #default="scope">
                    <div class="class-cell">{{ scope.row.bookingClass }}</div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-col>

        <!-- 右侧AI助手区域 -->
        <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8" class="assistant-section">
          <div class="section-card chat-card">
            <div class="section-header">
              <h2 class="section-title">AI助手</h2>
              <div class="online-status">
                <div class="status-dot"></div>
                在线服务
              </div>
            </div>

            <!-- ConversationId 管理区域 -->
            <div class="conversation-id-section">
              <div class="conversation-header">
                <span class="conversation-label">会话ID</span>
                <el-button
                    size="small"
                    type="text"
                    @click="showConversationInput = !showConversationInput"
                    class="toggle-input-btn"
                >
                  {{ showConversationInput ? '取消' : '切换会话' }}
                </el-button>
              </div>

              <div v-if="!showConversationInput" class="current-conversation">
                <div class="conversation-id-display">
                  <span class="conversation-id-text">{{ conversationId }}</span>
                  <el-button
                      size="small"
                      type="text"
                      @click="copyConversationId"
                      class="copy-btn"
                      :icon="DocumentCopy"
                  >
                    复制
                  </el-button>
                </div>
              </div>

              <div v-else class="conversation-input">
                <el-input
                    v-model="inputConversationId"
                    placeholder="输入会话ID以继续之前的聊天"
                    size="small"
                    class="conversation-input-field"
                />
                <div class="conversation-actions">
                  <el-button
                      size="small"
                      type="primary"
                      @click="switchConversation"
                      :disabled="!inputConversationId.trim()"
                  >
                    切换
                  </el-button>
                  <el-button
                      size="small"
                      @click="generateNewConversation"
                  >
                    新建会话
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 快捷功能按钮 -->
            <div class="quick-actions">
              <div class="quick-action-btn" @click="sendQuickMessage('查询我预订的机票')">
                <div class="action-icon">📋</div>
                <div class="action-text">查询预订</div>
              </div>
              <div class="quick-action-btn" @click="sendQuickMessage('我想修改我预订的机票')">
                <div class="action-icon">✏️</div>
                <div class="action-text">修改预订</div>
              </div>
              <div class="quick-action-btn" @click="sendQuickMessage('我要退订机票')">
                <div class="action-icon">⏰</div>
                <div class="action-text">退订机票</div>
              </div>
              <div class="quick-action-btn" @click="sendQuickMessage('我要预定机票')">
                <div class="action-icon">📞</div>
                <div class="action-text">预定机票</div>
              </div>
            </div>

            <!-- 聊天消息区域 -->
            <div class="chat-messages" ref="chatContainer">
              <div class="messages-container">
                <div
                    v-for="(activity, index) in activities"
                    :key="index"
                    :class="['message-item', activity.icon ? 'user-message' : 'ai-message']"
                >
                  <div class="message-avatar">
                    <div v-if="activity.icon" class="user-avatar">👤</div>
                    <div v-else class="ai-avatar">🤖</div>
                  </div>
                  <div class="message-content">
                    <div class="message-bubble">
                      <div class="message-text">{{ activity.content }}</div>
                    </div>
                    <div class="message-time">{{ activity.timestamp }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="chat-input-area">
              <div class="input-container">
                <el-input
                    v-model="msg"
                    class="chat-input"
                    placeholder="请输入您的问题..."
                    :rows="1"
                    type="textarea"
                    resize="none"
                    @keydown.enter.prevent="sendMsg"
                />
                <el-button
                    type="primary"
                    class="send-button"
                    :icon="Promotion"
                    @click="sendMsg"
                    :disabled="!msg.trim()"
                >
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script lang="ts">
import { Promotion, Search, DocumentCopy } from '@element-plus/icons-vue'
import { ref, onMounted, nextTick, computed, onBeforeMount } from "vue";
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  setup() {
    const activities = ref([
      {
        content: '欢迎来到乐山航空！我是您的专属AI助手，请问有什么可以帮您的？',
        timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
        color: '#0bbd87',
      },
    ]);
    const msg = ref('');
    const tableData = ref([]);
    const statusFilter = ref('all');
    const searchText = ref('');
    const chatContainer = ref(null);
    const conversationId = ref('');
    const inputConversationId = ref('');
    const showConversationInput = ref(false);
    const isMobile = ref(false);
    let count = 2;
    let eventSource;

    // 生成随机conversationId
    const generateConversationId = () => {
      const timestamp = Date.now();
      const random = Math.random().toString(36).substring(2, 15);
      return `conv_${timestamp}_${random}`;
    };

    // 检测是否为移动设备
    const checkMobile = () => {
      isMobile.value = window.innerWidth <= 768;
    };

    // 计算属性
    const confirmedBookings = computed(() => {
      return tableData.value.filter(booking => booking.bookingStatus === 'CONFIRMED').length;
    });

    const completedBookings = computed(() => {
      return tableData.value.filter(booking => booking.bookingStatus === 'COMPLETED').length;
    });

    const cancelledBookings = computed(() => {
      return tableData.value.filter(booking => booking.bookingStatus === 'CANCELLED').length;
    });

    const totalFlights = computed(() => {
      return tableData.value.length;
    });

    const filteredTableData = computed(() => {
      let filtered = tableData.value;

      // 状态筛选
      if (statusFilter.value !== 'all') {
        filtered = filtered.filter(booking => booking.bookingStatus === statusFilter.value);
      }

      // 搜索筛选
      if (searchText.value.trim()) {
        const searchTerm = searchText.value.trim().toLowerCase();
        filtered = filtered.filter(booking =>
            booking.bookingNumber.toLowerCase().includes(searchTerm) ||
            booking.name.toLowerCase().includes(searchTerm)
        );
      }

      return filtered;
    });

    // 状态类型映射
    const getStatusType = (status) => {
      switch (status) {
        case 'CONFIRMED':
          return 'success';
        case 'COMPLETED':
          return 'info';
        case 'CANCELLED':
          return 'danger';
        default:
          return 'warning';
      }
    };

    // 状态文本映射
    const getStatusText = (status) => {
      switch (status) {
        case 'CONFIRMED':
          return '已确认';
        case 'COMPLETED':
          return '已完成';
        case 'CANCELLED':
          return '已取消';
        default:
          return '待确认';
      }
    };

    // 复制会话ID
    const copyConversationId = async () => {
      try {
        await navigator.clipboard.writeText(conversationId.value);
        ElMessage.success('会话ID已复制到剪贴板');
      } catch (err) {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = conversationId.value;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        ElMessage.success('会话ID已复制到剪贴板');
      }
    };

    // 切换会话
    const switchConversation = () => {
      if (!inputConversationId.value.trim()) {
        ElMessage.warning('请输入有效的会话ID');
        return;
      }

      conversationId.value = inputConversationId.value.trim();
      showConversationInput.value = false;
      inputConversationId.value = '';

      // 清空当前聊天记录
      activities.value = [
        {
          content: `已切换到会话 ${conversationId.value}`,
          timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
          color: '#0bbd87',
        },
      ];
      count = 2;

      // 重新获取预订数据
      getBookings();

      ElMessage.success('会话已切换');
      console.log('Switched to conversationId:', conversationId.value);
    };

    // 生成新会话
    const generateNewConversation = () => {
      conversationId.value = generateConversationId();
      showConversationInput.value = false;
      inputConversationId.value = '';

      // 重置聊天记录
      activities.value = [
        {
          content: '欢迎来到乐山航空！我是您的专属AI助手，我可以帮您订票、退票、改票，还能查询城市天气和出行路线，请问需要什么服务呢？',
          timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
          color: '#0bbd87',
        },
      ];
      count = 2;

      // 重新获取预订数据
      getBookings();

      ElMessage.success('新会话已创建');
      console.log('Generated new conversationId:', conversationId.value);
    };

    const sendMsg = () => {
      if (!msg.value.trim()) return;

      if (eventSource) {
        eventSource.close();
      }

      activities.value.push(
          {
            content: msg.value,
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            size: 'large',
            type: 'primary',
            icon: true, // 标记为用户消息
          },
      );

      activities.value.push(
          {
            content: '正在思考中...',
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            color: '#0bbd87',
          },
      );

      // 滚动到底部
      nextTick(() => {
        scrollToBottom();
      });

      // sse: 服务端推送 Server-Sent Events，包含conversationId
      const url = `http://localhost:8080/ai/generateStreamAsString?message=${encodeURIComponent(msg.value)}&conversationId=${conversationId.value}`;
      eventSource = new EventSource(url);
      msg.value = '';

      eventSource.onmessage = (event) => {
        if (event.data === '[complete]') {
          count = count + 2;
          eventSource.close();
          getBookings();  // 每次对话完后刷新列表
          return;
        }
        activities.value[count].content += event.data;
        // 实时滚动到底部
        nextTick(() => {
          scrollToBottom();
        });
      };

      eventSource.onopen = (event) => {
        activities.value[count].content = '';
      };
    };

    const sendQuickMessage = (message) => {
      msg.value = message;
      sendMsg();
    };

    const scrollToBottom = () => {
      if (chatContainer.value) {
        const container = chatContainer.value;
        container.scrollTop = container.scrollHeight;
      }
    };

    const getBookings = () => {
      // 在获取预订列表时也可以传递conversationId
      const url = `http://localhost:8080/booking/list?conversationId=${conversationId.value}`;
      axios.get(url)
          .then((response) => {
            tableData.value = response.data;
          })
          .catch((error) => {
            console.error(error);
          });
    };

    // 监听窗口大小变化
    const handleResize = () => {
      checkMobile();
    };

    // 组件挂载前生成conversationId
    onBeforeMount(() => {
      conversationId.value = generateConversationId();
      console.log('Generated conversationId:', conversationId.value);
    });

    // Use onMounted to call getBookings when the component is mounted
    onMounted(() => {
      checkMobile();
      window.addEventListener('resize', handleResize);
      getBookings();
    });

    return {
      activities,
      msg,
      tableData,
      statusFilter,
      searchText,
      chatContainer,
      conversationId,
      inputConversationId,
      showConversationInput,
      isMobile,
      confirmedBookings,
      completedBookings,
      cancelledBookings,
      totalFlights,
      filteredTableData,
      sendMsg,
      sendQuickMessage,
      getBookings,
      getStatusType,
      getStatusText,
      copyConversationId,
      switchConversation,
      generateNewConversation,
      Promotion,
      Search,
      DocumentCopy,
    };
  },
};
</script>

<style scoped>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 主容器 */
.airline-assistant {
  min-height: 100vh;
  background: linear-gradient(135deg, #e0f2fe 0%, #bfdbfe 50%, #1e40af 100%);
  font-family: 'Inter', 'Helvetica Neue', sans-serif;
}

/* 页面头部 */
.page-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  padding: 16px 0;
  margin-bottom: 20px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-size: 28px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-3px); }
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a8a;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #1e3a8a;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

/* 主要内容区域 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.content-row {
  margin: 0 !important;
}

/* 卡片样式 */
.section-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  overflow: hidden;
  margin-bottom: 16px;
  height: 600px;
  display: flex;
  flex-direction: column;
}

.chat-card {
  background: rgba(255, 255, 255, 0.98);
}

/* 区域头部 */
.section-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(248, 250, 252, 0.8);
  flex-shrink: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a8a;
  margin: 0;
}

.booking-filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 200px;
}

.online-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #10b981;
  font-size: 12px;
  font-weight: 500;
}

.status-dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ConversationId 管理区域 */
.conversation-id-section {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.conversation-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.toggle-input-btn {
  font-size: 12px;
  padding: 4px 8px;
  color: #3b82f6;
}

.current-conversation {
  display: flex;
  align-items: center;
}

.conversation-id-display {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.conversation-id-text {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 11px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.copy-btn {
  font-size: 11px;
  padding: 4px 6px;
  color: #6b7280;
}

.conversation-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conversation-input-field {
  width: 100%;
}

.conversation-input-field :deep(.el-input__inner) {
  font-size: 12px;
  padding: 6px 8px;
}

.conversation-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.conversation-actions .el-button {
  font-size: 12px;
  padding: 6px 12px;
}

/* 预订统计卡片 - 移到上方 */
.booking-stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  flex-shrink: 0;
}

.stats-card {
  background: white;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stats-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
}

.stats-icon.success {
  background: #dcfce7;
  color: #16a34a;
}

.stats-icon.completed {
  background: #dbeafe;
  color: #2563eb;
}

.stats-icon.cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.stats-icon.info {
  background: #f3f4f6;
  color: #6b7280;
}

.stats-number {
  font-size: 18px;
  font-weight: 700;
  color: #1e3a8a;
}

.stats-label {
  font-size: 11px;
  color: #64748b;
}

/* 表格容器 */
.table-container {
  flex: 1;
  padding: 0 20px 20px;
  overflow: hidden;
}

.booking-table {
  border-radius: 6px;
  overflow: hidden;
  font-size: 13px;
}

.booking-table :deep(.el-table__header) {
  background: #f8fafc;
}

.booking-table :deep(.el-table__row) {
  transition: all 0.2s ease;
}

.booking-table :deep(.el-table__row:hover) {
  background-color: #f0f9ff !important;
}

.booking-table :deep(.el-table__cell) {
  padding: 10px 8px;
}

/* 表格单元格样式 */
.booking-number {
  font-weight: 600;
  color: #1e3a8a;
  font-size: 13px;
}

.date-cell, .location-cell, .class-cell {
  font-size: 13px;
  color: #374151;
}

.status-tag {
  font-weight: 500;
  border-radius: 4px;
  font-size: 11px;
}

/* 快捷功能按钮 */
.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.quick-action-btn {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
}

.quick-action-btn:hover {
  background: #f0f9ff;
  border-color: #3b82f6;
  transform: translateY(-1px);
}

.action-icon {
  font-size: 14px;
}

.action-text {
  font-weight: 500;
  color: #374151;
}

/* 聊天区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f8fafc;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  gap: 8px;
  animation: fadeInUp 0.3s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.user-avatar, .ai-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.user-avatar {
  background: linear-gradient(135deg, #3b82f6, #1e40af);
  color: white;
}

.ai-avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.message-content {
  flex: 1;
  max-width: 75%;
}

.user-message .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  margin-bottom: 2px;
  word-wrap: break-word;
}

.user-message .message-bubble {
  background: linear-gradient(135deg, #3b82f6, #1e40af);
  color: white;
  border-bottom-right-radius: 3px;
}

.ai-message .message-bubble {
  background: white;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 3px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.message-text {
  line-height: 1.4;
  font-size: 13px;
}

.message-time {
  font-size: 10px;
  color: #9ca3af;
  padding: 0 4px;
}

/* 输入区域 */
.chat-input-area {
  padding: 12px;
  background: white;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.input-container {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.4;
  resize: none;
  transition: all 0.2s ease;
}

.chat-input :deep(.el-textarea__inner):focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.send-button {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #3b82f6, #1e40af);
  border: none;
  transition: all 0.2s ease;
  font-size: 13px;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  .header-stats {
    gap: 16px;
    flex-wrap: wrap;
    justify-content: center;
  }

  .booking-filters {
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .booking-stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }

  .section-card {
    height: auto;
    min-height: 400px;
  }

  .chat-card {
    height: 500px;
  }

  /* 移动端表格优化 */
  .booking-table {
    font-size: 12px;
  }

  .booking-table :deep(.el-table__cell) {
    padding: 8px 4px;
  }

  .stats-card {
    padding: 8px;
  }

  .stats-number {
    font-size: 16px;
  }

  .stats-label {
    font-size: 10px;
  }

  /* 移动端会话ID区域优化 */
  .conversation-id-text {
    font-size: 10px;
  }

  .conversation-actions {
    flex-direction: column;
  }

  .conversation-actions .el-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 0 10px;
  }

  .header-content {
    padding: 0 10px;
  }

  .title {
    font-size: 20px;
  }

  .logo {
    font-size: 24px;
  }

  .section-header {
    padding: 12px 16px;
  }

  .booking-stats-cards {
    padding: 12px 16px;
    gap: 8px;
  }

  .table-container {
    padding: 0 16px 16px;
  }

  .conversation-id-section {
    padding: 8px 12px;
  }
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 2px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>

