package com.blockphonespam.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blockphonespam.data.local.BlockedCallEntity
import com.blockphonespam.data.local.PrefixRuleEntity
import com.blockphonespam.util.OemAutoStartHelper
import com.blockphonespam.util.ProtectionPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onTogglePrefix: (String, Boolean) -> Unit,
    onNewPrefixChange: (String) -> Unit,
    onAddPrefix: () -> Unit,
    onDeletePrefix: (String) -> Unit,
    onClearBlockedCalls: () -> Unit,
    onRequestCallScreeningRole: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onOpenAutoStartSettings: () -> Unit
) {
    var showPermissionsPanel by remember(state.permissionState.allCoreGranted) {
        mutableStateOf(!state.permissionState.allCoreGranted)
    }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Chặn Cuộc Gọi Rác",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val statusText = if (state.permissionState.allCoreGranted && state.isServiceRunning) {
                            "Đang chạy ngầm bảo vệ"
                        } else if (state.permissionState.hasCallScreeningRole) {
                            "Đang lọc cuộc gọi • Kiểm tra quyền nền"
                        } else {
                            "Chưa cấp đủ quyền hệ thống"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.permissionState.allCoreGranted) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPermissionsPanel = !showPermissionsPanel }) {
                        Icon(
                            imageVector = if (state.permissionState.allCoreGranted) {
                                Icons.Default.Security
                            } else {
                                Icons.Default.WarningAmber
                            },
                            contentDescription = "Cấu hình quyền chạy ngầm",
                            tint = if (state.permissionState.allCoreGranted) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Bảng cấp quyền hệ thống chống kill nền (tự mở khi thiếu quyền, bấm icon khiên để ẩn/hiện)
            AnimatedVisibility(visible = showPermissionsPanel) {
                SystemPermissionsCard(
                    permissionState = state.permissionState,
                    onRequestCallScreeningRole = onRequestCallScreeningRole,
                    onRequestBatteryOptimization = onRequestBatteryOptimization,
                    onRequestRuntimePermissions = onRequestRuntimePermissions,
                    onOpenAutoStartSettings = onOpenAutoStartSettings
                )
            }

            // PHẦN TRÊN: Table hiển thị danh sách cuộc gọi đã bị chặn
            BlockedCallsTableSection(
                blockedCalls = state.blockedCalls,
                onRequestClearAll = { showClearDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // PHẦN DƯỚI: Component quản lý các đầu số chặn (tick để bật/tắt, thêm/xóa)
            PrefixManagementSection(
                prefixes = state.prefixes,
                newPrefixInput = state.newPrefixInput,
                prefixError = state.prefixError,
                onTogglePrefix = onTogglePrefix,
                onNewPrefixChange = onNewPrefixChange,
                onAddPrefix = onAddPrefix,
                onDeletePrefix = onDeletePrefix,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Xóa lịch sử chặn?") },
            text = { Text("Toàn bộ danh sách cuộc gọi đã bị chặn sẽ bị xóa khỏi bảng.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearBlockedCalls()
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SystemPermissionsCard(
    permissionState: ProtectionPermissionState,
    onRequestCallScreeningRole: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onRequestRuntimePermissions: () -> Unit,
    onOpenAutoStartSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Quyền hệ thống & Chạy ngầm chống tắt app",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            PermissionActionRow(
                label = "Dịch vụ chặn cuộc gọi (Call Screening)",
                granted = permissionState.hasCallScreeningRole,
                actionLabel = "Cấp quyền",
                onClick = onRequestCallScreeningRole
            )

            PermissionActionRow(
                label = "Không tối ưu pin (Chống kill nền)",
                granted = permissionState.isIgnoringBatteryOptimizations,
                actionLabel = "Bỏ giới hạn",
                onClick = onRequestBatteryOptimization
            )

            PermissionActionRow(
                label = "Quyền điện thoại & Thông báo nền",
                granted = permissionState.hasPhonePermissions && permissionState.hasNotificationPermission,
                actionLabel = "Cấp quyền",
                onClick = onRequestRuntimePermissions
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tự khởi chạy nền (${OemAutoStartHelper.deviceBrandLabel()})",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onOpenAutoStartSettings,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Mở cài đặt", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PermissionActionRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (granted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (granted) {
            Text(
                text = "Đã bật",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        } else {
            Button(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun BlockedCallsTableSection(
    blockedCalls: List<BlockedCallEntity>,
    onRequestClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tiêu đề bảng
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneDisabled,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Danh sách cuộc gọi bị chặn (${blockedCalls.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (blockedCalls.isNotEmpty()) {
                    TextButton(
                        onClick = onRequestClearAll,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Xóa tất cả",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa hết", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Header của Table
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TableHeaderCell("STT", Modifier.weight(0.13f), TextAlign.Center)
                TableHeaderCell("Số điện thoại", Modifier.weight(0.39f), TextAlign.Start)
                TableHeaderCell("Đầu số", Modifier.weight(0.18f), TextAlign.Center)
                TableHeaderCell("Thời gian", Modifier.weight(0.30f), TextAlign.End)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Nội dung Table
            if (blockedCalls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có cuộc gọi nào bị chặn.\nCác số thuộc đầu số đang tick bên dưới sẽ tự động bị từ chối và hiển thị tại đây.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(
                        items = blockedCalls,
                        key = { _, item -> item.id }
                    ) { index, call ->
                        val rowBg = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.13f)
                            )
                            Text(
                                text = call.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.39f)
                            )
                            Box(
                                modifier = Modifier.weight(0.18f),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = call.matchedPrefix,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = dateFormatter.format(Date(call.blockedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(0.30f)
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier,
    textAlign: TextAlign
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = textAlign,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrefixManagementSection(
    prefixes: List<PrefixRuleEntity>,
    newPrefixInput: String,
    prefixError: String?,
    onTogglePrefix: (String, Boolean) -> Unit,
    onNewPrefixChange: (String) -> Unit,
    onAddPrefix: () -> Unit,
    onDeletePrefix: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledCount = prefixes.count { it.isEnabled }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quản lý đầu số chặn",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$enabledCount/${prefixes.size} đang bật",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Danh sách các đầu số dạng checkbox để tick bật/tắt
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                prefixes.forEach { rule ->
                    PrefixCheckboxItem(
                        rule = rule,
                        onToggle = { checked -> onTogglePrefix(rule.prefix, checked) },
                        onDelete = if (!rule.isDefault) {
                            { onDeletePrefix(rule.prefix) }
                        } else {
                            null
                        }
                    )
                }
            }

            // Ô nhập để thêm đầu số mới
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newPrefixInput,
                    onValueChange = onNewPrefixChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Thêm đầu số mới (VD: 024, 029)") },
                    singleLine = true,
                    isError = prefixError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onAddPrefix() }
                    )
                )
                Button(
                    onClick = onAddPrefix,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm")
                }
            }

            if (prefixError != null) {
                Text(
                    text = prefixError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PrefixCheckboxItem(
    rule: PrefixRuleEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val borderColor = if (rule.isEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    }
    val containerColor = if (rule.isEnabled) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onToggle(!rule.isEnabled) }
            .padding(start = 2.dp, end = if (onDelete != null) 4.dp else 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = rule.prefix,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Xóa đầu số ${rule.prefix}",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
