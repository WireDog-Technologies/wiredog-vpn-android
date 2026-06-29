package com.wiredog.vpn.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.wiredog.vpn.ui.theme.VpnBackground
import com.wiredog.vpn.ui.theme.VpnCardBackground
import com.wiredog.vpn.ui.theme.VpnDividerColor
import com.wiredog.vpn.ui.theme.VpnGreen
import com.wiredog.vpn.ui.theme.VpnPrimary
import com.wiredog.vpn.ui.theme.VpnRed
import com.wiredog.vpn.ui.theme.VpnSecondaryBackground
import com.wiredog.vpn.ui.theme.VpnTextPrimary
import com.wiredog.vpn.ui.theme.VpnTextSecondary

private enum class SplitTunnelingPage { Main, Apps, IpAddresses }

@Composable
fun SplitTunnelingScreen(
    onNavigateBack: () -> Unit,
    viewModel: SplitTunnelingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SplitTunnelingPage.Main) }
    var showReconnectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.showReconnectDialog.collect {
            showReconnectDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            onNavigateBack()
        }
    }

    if (showReconnectDialog) {
        AlertDialog(
            onDismissRequest = { showReconnectDialog = false },
            title = { Text("Split Tunneling Updated", color = VpnTextPrimary) },
            text = { Text("Settings saved. Would you like to reconnect now for changes to take effect?", color = VpnTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showReconnectDialog = false
                    viewModel.reconnectNow()
                }) {
                    Text("Reconnect Now", color = VpnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReconnectDialog = false
                    onNavigateBack()
                }) {
                    Text("Apply Next Time", color = VpnTextSecondary)
                }
            },
            containerColor = VpnCardBackground
        )
    }

    when (currentPage) {
        SplitTunnelingPage.Main -> MainPage(
            uiState = uiState,
            onBack = onNavigateBack,
            onSave = { viewModel.save() },
            onToggle = { viewModel.setEnabled(it) },
            onModeChanged = { viewModel.setMode(it) },
            onAppsClick = { currentPage = SplitTunnelingPage.Apps },
            onIpsClick = { currentPage = SplitTunnelingPage.IpAddresses }
        )
        SplitTunnelingPage.Apps -> AppsPage(
            uiState = uiState,
            onBack = { currentPage = SplitTunnelingPage.Main },
            onToggleApp = { viewModel.toggleApp(it) },
            onSearchChanged = { viewModel.setSearchQuery(it) }
        )
        SplitTunnelingPage.IpAddresses -> IpAddressesPage(
            uiState = uiState,
            onBack = { currentPage = SplitTunnelingPage.Main },
            onAdd = { viewModel.addIpAddress(it) },
            onUpdate = { index, ip -> viewModel.updateIpAddress(index, ip) },
            onRemove = { viewModel.removeIpAddress(it) },
            isValid = { viewModel.isValidIpAddress(it) }
        )
    }
}

@Composable
private fun MainPage(
    uiState: SplitTunnelingUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onModeChanged: (String) -> Unit,
    onAppsClick: () -> Unit,
    onIpsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        TopBar(
            title = "Split Tunneling",
            onBack = onBack,
            saveButton = {
                TextButton(
                    onClick = onSave,
                    enabled = uiState.hasUnsavedChanges
                ) {
                    Text(
                        text = "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.hasUnsavedChanges) VpnPrimary else VpnTextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Split Tunneling",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = VpnTextPrimary
                        )
                        Text(
                            text = "Route specific apps or IPs outside the VPN",
                            fontSize = 13.sp,
                            color = VpnTextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VpnTextPrimary,
                            checkedTrackColor = VpnGreen,
                            uncheckedThumbColor = VpnTextPrimary,
                            uncheckedTrackColor = VpnTextSecondary.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            if (uiState.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("MODE")

                SectionCard {
                    ModeSelector(
                        currentMode = uiState.mode,
                        onModeChanged = onModeChanged
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("CONFIGURATION")
                SectionCard {
                    val appsLabel = if (uiState.mode == "exclude") "Excluded Apps" else "Included Apps"
                    val appsCount = uiState.selectedApps.size
                    val appsSummary = if (appsCount == 0) "None selected"
                        else "$appsCount app${if (appsCount != 1) "s" else ""} selected"

                    NavigationRow(
                        icon = Icons.Default.Apps,
                        iconTint = VpnPrimary,
                        title = appsLabel,
                        subtitle = appsSummary,
                        onClick = onAppsClick
                    )

                    HorizontalDivider(color = VpnDividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 52.dp))

                    val ipsLabel = if (uiState.mode == "exclude") "Excluded IP Addresses" else "Included IP Addresses"
                    val ipsCount = uiState.ipAddresses.size
                    val ipsSummary = if (ipsCount == 0) "None added"
                        else "$ipsCount address${if (ipsCount != 1) "es" else ""}"

                    NavigationRow(
                        icon = Icons.Default.Language,
                        iconTint = VpnGreen,
                        title = ipsLabel,
                        subtitle = ipsSummary,
                        onClick = onIpsClick
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = VpnTextPrimary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = VpnTextSecondary,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Open",
            tint = VpnTextSecondary
        )
    }
}

@Composable
private fun AppsPage(
    uiState: SplitTunnelingUiState,
    onBack: () -> Unit,
    onToggleApp: (String) -> Unit,
    onSearchChanged: (String) -> Unit
) {
    val title = if (uiState.mode == "exclude") "Excluded Apps" else "Included Apps"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        TopBar(title = title, onBack = onBack)

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VpnPrimary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (uiState.selectedApps.isNotEmpty()) {
                    item {
                        SectionHeader(
                            "${uiState.selectedApps.size} SELECTED APP${if (uiState.selectedApps.size != 1) "S" else ""}"
                        )
                    }

                    val selectedAppInfos = uiState.allApps.filter {
                        uiState.selectedApps.contains(it.packageName)
                    }

                    items(
                        items = selectedAppInfos,
                        key = { "selected_${it.packageName}" }
                    ) { app ->
                        AppRow(
                            app = app,
                            isSelected = true,
                            onToggle = { onToggleApp(app.packageName) }
                        )
                    }
                }

                item {
                    SectionHeader("ALL APPS")
                    SectionCard {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search apps...", color = VpnTextSecondary) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = VpnTextSecondary)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChanged("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = VpnTextSecondary)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VpnTextPrimary,
                                unfocusedTextColor = VpnTextPrimary,
                                cursorColor = VpnPrimary,
                                focusedBorderColor = VpnPrimary,
                                unfocusedBorderColor = VpnDividerColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                val filteredApps = uiState.allApps.filter {
                    uiState.searchQuery.isEmpty() ||
                            it.name.contains(uiState.searchQuery, ignoreCase = true) ||
                            it.packageName.contains(uiState.searchQuery, ignoreCase = true)
                }

                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    AppRow(
                        app = app,
                        isSelected = uiState.selectedApps.contains(app.packageName),
                        onToggle = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IpAddressesPage(
    uiState: SplitTunnelingUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onUpdate: (Int, String) -> Unit,
    onRemove: (Int) -> Unit,
    isValid: (String) -> Boolean
) {
    val title = if (uiState.mode == "exclude") "Excluded IP Addresses" else "Included IP Addresses"
    var newIp by remember { mutableStateOf("") }
    var ipError by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VpnBackground)
    ) {
        TopBar(title = title, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newIp,
                            onValueChange = {
                                newIp = it
                                ipError = false
                            },
                            placeholder = { Text("192.168.1.0/24", fontSize = 13.sp, color = VpnTextSecondary) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = ipError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newIp.isNotBlank()) {
                                        if (isValid(newIp)) {
                                            onAdd(newIp)
                                            newIp = ""
                                            focusManager.clearFocus()
                                        } else {
                                            ipError = true
                                        }
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VpnTextPrimary,
                                unfocusedTextColor = VpnTextPrimary,
                                cursorColor = VpnPrimary,
                                focusedBorderColor = VpnPrimary,
                                unfocusedBorderColor = VpnDividerColor,
                                errorBorderColor = VpnRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(fontSize = 14.sp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (newIp.isNotBlank()) {
                                    if (isValid(newIp)) {
                                        onAdd(newIp)
                                        newIp = ""
                                        focusManager.clearFocus()
                                    } else {
                                        ipError = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add IP",
                                tint = VpnPrimary
                            )
                        }
                    }

                    if (ipError) {
                        Text(
                            text = "Invalid IP address format",
                            fontSize = 12.sp,
                            color = VpnRed,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (uiState.ipAddresses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("${uiState.ipAddresses.size} ADDRESS${if (uiState.ipAddresses.size != 1) "ES" else ""}")
                SectionCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        uiState.ipAddresses.forEachIndexed { index, ip ->
                            if (editingIndex == index) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editingValue,
                                        onValueChange = { editingValue = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Uri,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                if (isValid(editingValue)) {
                                                    onUpdate(index, editingValue)
                                                    editingIndex = null
                                                    focusManager.clearFocus()
                                                }
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = VpnTextPrimary,
                                            unfocusedTextColor = VpnTextPrimary,
                                            cursorColor = VpnPrimary,
                                            focusedBorderColor = VpnPrimary,
                                            unfocusedBorderColor = VpnDividerColor
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = TextStyle(fontSize = 14.sp)
                                    )
                                    IconButton(onClick = {
                                        if (isValid(editingValue)) {
                                            onUpdate(index, editingValue)
                                            editingIndex = null
                                            focusManager.clearFocus()
                                        }
                                    }) {
                                        Text("OK", color = VpnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = ip,
                                        fontSize = 14.sp,
                                        color = VpnTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            editingIndex = index
                                            editingValue = ip
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = VpnTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemove(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = VpnRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (index < uiState.ipAddresses.lastIndex) {
                                    HorizontalDivider(color = VpnDividerColor, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    saveButton: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VpnBackground)
            .padding(start = 4.dp, end = if (saveButton != null) 16.dp else 4.dp, top = 36.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VpnTextPrimary
            )
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VpnTextPrimary,
            modifier = Modifier.weight(1f)
        )

        if (saveButton != null) {
            saveButton()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VpnSecondaryBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VpnTextSecondary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VpnCardBackground)
    ) {
        Column { content() }
    }
}

@Composable
private fun ModeSelector(
    currentMode: String,
    onModeChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeButton(
            label = "Include",
            description = "Only selected apps use VPN",
            isSelected = currentMode == "include",
            onClick = { onModeChanged("include") },
            modifier = Modifier.weight(1f)
        )
        ModeButton(
            label = "Exclude",
            description = "Selected apps bypass VPN",
            isSelected = currentMode == "exclude",
            onClick = { onModeChanged("exclude") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) VpnPrimary.copy(alpha = 0.15f) else VpnSecondaryBackground)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) VpnPrimary else VpnTextSecondary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = if (isSelected) VpnPrimary.copy(alpha = 0.7f) else VpnTextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VpnCardBackground)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = app.icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(VpnSecondaryBackground, RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = app.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = VpnTextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = VpnPrimary,
                uncheckedColor = VpnTextSecondary,
                checkmarkColor = VpnTextPrimary
            )
        )
    }

    HorizontalDivider(color = VpnDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
}
