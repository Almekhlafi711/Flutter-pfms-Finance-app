package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.theme.GreenIncome
import com.example.ui.theme.RedExpense
import com.example.ui.viewmodel.PfmsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    viewModel: PfmsViewModel,
    onNavigateBack: () -> Unit
) {
    val assets by viewModel.assets.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isArabic by viewModel.isArabic.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var selectedAssetId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("ACTIVE") } // "ACTIVE", "SOLD", "ALL"

    val categoriesEn = remember { listOf("All", "Real Estate", "Vehicles", "Investments", "Gold", "Crypto", "Business", "Electronics", "Other") }
    val categoriesAr = remember { listOf("الكل", "عقارات", "مركبات", "استثمارات", "ذهب", "عملات رقمية", "أعمال", "إلكترونيات", "أخرى") }
    val currentCategories = remember(isArabic) { if (isArabic) categoriesAr else categoriesEn }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { currentCategories.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Memoized Filtering Per Category for smooth 60fps/120fps swiping
    val filteredAssetsByCategory by remember(assets, searchQuery, statusFilter, isArabic, currentCategories) {
        derivedStateOf {
            currentCategories.associateWith { category ->
                assets.filter { asset ->
                    val matchesSearch = searchQuery.isBlank() ||
                            asset.name.contains(searchQuery, ignoreCase = true) ||
                            asset.type.name.contains(searchQuery, ignoreCase = true) ||
                            asset.notes.contains(searchQuery, ignoreCase = true)

                    val matchesStatus = when (statusFilter) {
                        "ACTIVE" -> asset.status == AssetStatus.ACTIVE
                        "SOLD" -> asset.status == AssetStatus.SOLD
                        else -> true
                    }

                    val mappedCategoryEn = when (asset.type) {
                        AssetType.REAL_ESTATE -> "Real Estate"
                        AssetType.VEHICLE -> "Vehicles"
                        AssetType.STOCKS, AssetType.INVESTMENTS -> "Investments"
                        AssetType.GOLD -> "Gold"
                        AssetType.CRYPTO -> "Crypto"
                        AssetType.PROJECT, AssetType.BUSINESS -> "Business"
                        AssetType.COLLECTIBLE, AssetType.ELECTRONICS -> "Electronics"
                        else -> "Other"
                    }
                    val mappedCategoryAr = when (asset.type) {
                        AssetType.REAL_ESTATE -> "عقارات"
                        AssetType.VEHICLE -> "مركبات"
                        AssetType.STOCKS, AssetType.INVESTMENTS -> "استثمارات"
                        AssetType.GOLD -> "ذهب"
                        AssetType.CRYPTO -> "عملات رقمية"
                        AssetType.PROJECT, AssetType.BUSINESS -> "أعمال"
                        AssetType.COLLECTIBLE, AssetType.ELECTRONICS -> "إلكترونيات"
                        else -> "أخرى"
                    }
                    val mappedCategory = if (isArabic) mappedCategoryAr else mappedCategoryEn
                    val allLabel = if (isArabic) "الكل" else "All"

                    val matchesCategory = category == allLabel || category == mappedCategory

                    matchesSearch && matchesStatus && matchesCategory
                }
            }
        }
    }

    // Pre-calculated Summary Stats for Each Category (Active Assets only)
    val categoryStatsMap by remember(assets, isArabic, currentCategories) {
        derivedStateOf {
            currentCategories.associateWith { category ->
                val allLabel = if (isArabic) "الكل" else "All"
                val catActiveAssets = assets.filter { asset ->
                    if (asset.status != AssetStatus.ACTIVE) return@filter false
                    if (category == allLabel) return@filter true

                    val mappedCategoryEn = when (asset.type) {
                        AssetType.REAL_ESTATE -> "Real Estate"
                        AssetType.VEHICLE -> "Vehicles"
                        AssetType.STOCKS, AssetType.INVESTMENTS -> "Investments"
                        AssetType.GOLD -> "Gold"
                        AssetType.CRYPTO -> "Crypto"
                        AssetType.PROJECT, AssetType.BUSINESS -> "Business"
                        AssetType.COLLECTIBLE, AssetType.ELECTRONICS -> "Electronics"
                        else -> "Other"
                    }
                    val mappedCategoryAr = when (asset.type) {
                        AssetType.REAL_ESTATE -> "عقارات"
                        AssetType.VEHICLE -> "مركبات"
                        AssetType.STOCKS, AssetType.INVESTMENTS -> "استثمارات"
                        AssetType.GOLD -> "ذهب"
                        AssetType.CRYPTO -> "عملات رقمية"
                        AssetType.PROJECT, AssetType.BUSINESS -> "أعمال"
                        AssetType.COLLECTIBLE, AssetType.ELECTRONICS -> "إلكترونيات"
                        else -> "أخرى"
                    }
                    val mappedCategory = if (isArabic) mappedCategoryAr else mappedCategoryEn
                    category == mappedCategory
                }

                val catCurrentValue = catActiveAssets.sumOf { it.totalCurrentValue }
                val catPurchaseCost = catActiveAssets.sumOf { it.totalPurchaseValue }
                val catProfit = catCurrentValue - catPurchaseCost
                val catProfitPct = if (catPurchaseCost > 0) (catProfit / catPurchaseCost) * 100 else 0.0
                CategorySummaryStats(catCurrentValue, catPurchaseCost, catProfit, catProfitPct)
            }
        }
    }

    // Active assets count for TopBar
    val activeAssetsCount by remember(assets) {
        derivedStateOf { assets.count { it.status == AssetStatus.ACTIVE } }
    }

    // Selected Asset Details Screen
    if (selectedAssetId != null) {
        val selectedAsset = assets.find { it.id == selectedAssetId }
        if (selectedAsset != null) {
            AssetDetailsScreen(
                asset = selectedAsset,
                viewModel = viewModel,
                accounts = accounts,
                isArabic = isArabic,
                onBack = { selectedAssetId = null },
                onDelete = { 
                    viewModel.deleteAsset(it.id)
                    selectedAssetId = null 
                }
            )
        } else {
            selectedAssetId = null
        }
        return
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (isArabic) "البحث في الأصول..." else "Search assets...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(25.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    isSearchActive = false 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        )
                    } else {
                        Column {
                            Text(
                                text = if (isArabic) "مركز الأصول" else "Assets Center",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isArabic) "$activeAssetsCount أصول نشطة" else "$activeAssetsCount active assets",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButtonPosition = if (isArabic) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Color(0xFF6B46C1),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Asset")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            // 💳 1. FULL-WIDTH PURPLE GRADIENT HERO SUMMARY CARD
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 0.dp,
                key = { pageIndex -> currentCategories.getOrNull(pageIndex) ?: pageIndex }
            ) { pageIndex ->
                val catName = currentCategories.getOrNull(pageIndex) ?: ""
                val stats = categoryStatsMap[catName] ?: CategorySummaryStats(0.0, 0.0, 0.0, 0.0)
                val catCurrentValue = stats.currentValue
                val catPurchaseCost = stats.purchaseCost
                val catProfit = stats.profit
                val catProfitPct = stats.profitPct

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(20.dp)
                        },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF6B46C1), Color(0xFF805AD5))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 25.dp, y = (-25).dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Glassmorphic Category Tag
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (catName == (if (isArabic) "الكل" else "All")) {
                                                if (isArabic) "جميع الأصول" else "All Assets"
                                            } else {
                                                catName
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                // Total Current Value (Prominent White Text)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isArabic) "إجمالي القيمة الحالية" else "Total Current Value",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "SAR ${String.format(Locale.US, "%,.2f", catCurrentValue)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 22.sp
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) "تكلفة الشراء" else "Purchase Cost",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "SAR ${String.format(Locale.US, "%,.2f", catPurchaseCost)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isArabic) "إجمالي الأرباح/العائد" else "Total Profit / Return",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${if (catProfit >= 0) "+" else ""}SAR ${String.format(Locale.US, "%,.2f", catProfit)}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.22f)
                                        ) {
                                            Text(
                                                text = "${if (catProfit >= 0) "+" else ""}${String.format(Locale.US, "%.1f", catProfitPct)}%",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔘 2. DYNAMIC DOTS INDICATOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(currentCategories.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF6B46C1) else Color(0xFFCBD5E1)
                            )
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                    )
                }
            }

            // Categories Header Scroll
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                currentCategories.forEachIndexed { index, category ->
                    val isSelected = pagerState.currentPage == index
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B46C1),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF4A5568)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color.Transparent else Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Assets List Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { pageIndex -> currentCategories.getOrNull(pageIndex) ?: pageIndex }
            ) { pageIndex ->
                val catName = currentCategories.getOrNull(pageIndex) ?: ""
                val currentCategoryAssets = filteredAssetsByCategory[catName] ?: emptyList()
                if (currentCategoryAssets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isArabic) "لا توجد أصول في هذه الفئة" else "No assets found in this category",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "اضغط على زر (+) لإضافة أصل جديد بالريال السعودي" else "Tap (+) button to add a new SAR asset",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentCategoryAssets, key = { it.id }) { asset ->
                            AssetCardItem(
                                asset = asset,
                                isArabic = isArabic,
                                onClick = { selectedAssetId = asset.id }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddAssetBottomSheet(
            viewModel = viewModel,
            accounts = accounts,
            isArabic = isArabic,
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
fun AssetCardItem(
    asset: Asset,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val profit = asset.netGainLoss
    val profitPercentage = if (asset.totalPurchaseValue > 0) (profit / asset.totalPurchaseValue) * 100 else 0.0

    val icon = when(asset.type) {
        AssetType.REAL_ESTATE -> Icons.Default.Home
        AssetType.VEHICLE -> Icons.Default.DirectionsCar
        AssetType.GOLD -> Icons.Default.Star
        AssetType.STOCKS, AssetType.INVESTMENTS -> Icons.Default.TrendingUp
        AssetType.CRYPTO -> Icons.Default.CurrencyBitcoin
        AssetType.PROJECT, AssetType.BUSINESS -> Icons.Default.Business
        AssetType.COLLECTIBLE, AssetType.ELECTRONICS -> Icons.Default.Devices
        else -> Icons.Default.Work
    }

    val categoryLabel = when(asset.type) {
        AssetType.REAL_ESTATE -> if (isArabic) "عقار" else "Real Estate"
        AssetType.VEHICLE -> if (isArabic) "مركبة" else "Vehicle"
        AssetType.GOLD -> if (isArabic) "ذهب" else "Gold"
        AssetType.STOCKS, AssetType.INVESTMENTS -> if (isArabic) "استثمار" else "Investment"
        AssetType.CRYPTO -> if (isArabic) "عملات رقمية" else "Crypto"
        AssetType.PROJECT, AssetType.BUSINESS -> if (isArabic) "مشروع" else "Business"
        AssetType.COLLECTIBLE, AssetType.ELECTRONICS -> if (isArabic) "إلكترونيات" else "Electronics"
        else -> if (isArabic) "أخرى" else "Other"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF6B46C1).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6B46C1),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.getLocalizedName(isArabic),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (asset.status == AssetStatus.SOLD) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Gray.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isArabic) "مباع" else "Sold",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.DarkGray)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryLabel • ${if (isArabic) "شراء:" else "Cost:"} SAR ${String.format(Locale.US, "%,.0f", asset.totalPurchaseValue)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SAR ${String.format(Locale.US, "%,.2f", asset.totalCurrentValue)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (profit >= 0) "+" else ""}SAR ${String.format(Locale.US, "%,.0f", profit)} (${if (profit >= 0) "+" else ""}${String.format(Locale.US, "%.1f", profitPercentage)}%)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (profit >= 0) GreenIncome else RedExpense
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsScreen(
    asset: Asset,
    viewModel: PfmsViewModel,
    accounts: List<Account>,
    isArabic: Boolean,
    onBack: () -> Unit,
    onDelete: (Asset) -> Unit
) {
    val logs by viewModel.getAssetLogs(asset.id).collectAsState(initial = emptyList())
    var showUpdateValueDialog by remember { mutableStateOf(false) }
    var showSellAssetDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val profit = asset.netGainLoss
    val profitPercentage = if (asset.totalPurchaseValue > 0) (profit / asset.totalPurchaseValue) * 100 else 0.0

    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الملف المالي للأصل" else "Asset Financial Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedExpense)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = asset.getLocalizedName(isArabic),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = asset.type.name,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (asset.status == AssetStatus.ACTIVE) Color(0xFF6B46C1).copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (asset.status == AssetStatus.ACTIVE) (if (isArabic) "نشط" else "Active") else (if (isArabic) "مباع" else "Sold"),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (asset.status == AssetStatus.ACTIVE) Color(0xFF6B46C1) else Color.DarkGray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(if (isArabic) "القيمة الحالية" else "Current Value", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            Text(
                                text = "SAR ${String.format(Locale.US, "%,.2f", asset.totalCurrentValue)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isArabic) "تكلفة الشراء" else "Purchase Cost", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            Text(
                                text = "SAR ${String.format(Locale.US, "%,.2f", asset.totalPurchaseValue)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(if (isArabic) "الربح / الخسارة" else "Profit / Loss", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            Text(
                                text = "${if (profit >= 0) "+" else ""}SAR ${String.format(Locale.US, "%,.2f", profit)} (${if (profit >= 0) "+" else ""}${String.format(Locale.US, "%.1f", profitPercentage)}%)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (profit >= 0) GreenIncome else RedExpense
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isArabic) "حساب الشراء" else "Purchase Account", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            Text(
                                text = asset.purchaseAccountName.ifEmpty { "-" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    if (asset.status == AssetStatus.SOLD && asset.soldPrice != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(if (isArabic) "سعر البيع" else "Sale Price", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text(
                                    text = "SAR ${String.format(Locale.US, "%,.2f", asset.soldPrice)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = GreenIncome)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (isArabic) "حساب التصفية" else "Liquidation Account", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                                Text(
                                    text = asset.soldAccountName ?: "-",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Bar (If Active)
            if (asset.status == AssetStatus.ACTIVE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showUpdateValueDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B46C1))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "تحديث القيمة" else "Update Value")
                    }

                    Button(
                        onClick = { showSellAssetDialog = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenIncome)
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "بيع / تصفية" else "Sell Asset")
                    }
                }
            }

            // Transaction History Logs Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) "سجل عمليات الأصل" else "Asset Transaction History Log",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (logs.isEmpty()) {
                        Text(
                            text = if (isArabic) "لا توجد سجلات بعد" else "No logs recorded yet",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                    } else {
                        logs.forEachIndexed { index, log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val logIcon = when(log.type) {
                                    AssetLogType.PURCHASE -> Icons.Default.ShoppingCart
                                    AssetLogType.VALUE_UPDATE -> Icons.Default.TrendingUp
                                    AssetLogType.SALE -> Icons.Default.Sell
                                }
                                val logColor = when(log.type) {
                                    AssetLogType.PURCHASE -> Color(0xFF6B46C1)
                                    AssetLogType.VALUE_UPDATE -> Color(0xFF0284C7)
                                    AssetLogType.SALE -> GreenIncome
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(logColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(logIcon, contentDescription = null, tint = logColor, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.date)),
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                    )
                                    if (log.notes.isNotBlank()) {
                                        Text(
                                            text = log.notes,
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "SAR ${String.format(Locale.US, "%,.2f", log.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (log.accountName.isNotBlank()) {
                                        Text(
                                            text = log.accountName,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                        )
                                    }
                                }
                            }
                            if (index < logs.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }
    }

    // Update Value Dialog
    if (showUpdateValueDialog) {
        var newValueStr by remember { mutableStateOf(asset.currentValue.toString()) }
        var updateNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUpdateValueDialog = false },
            title = { Text(if (isArabic) "تحديث قيمة الأصل" else "Update Asset Value") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "أدخل القيمة التقديرية الحالية للأصل (بالريال السعودي):" else "Enter current estimated value (in SAR):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newValueStr,
                        onValueChange = { newValueStr = it },
                        label = { Text(if (isArabic) "القيمة الحالية الجديدة (SAR)" else "New Current Value (SAR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = updateNotes,
                        onValueChange = { updateNotes = it },
                        label = { Text(if (isArabic) "ملاحظات التحديث" else "Update Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newVal = newValueStr.toDoubleOrNull() ?: asset.currentValue
                        viewModel.updateAssetValue(asset, newVal, updateNotes)
                        showUpdateValueDialog = false
                    }
                ) {
                    Text(if (isArabic) "حفظ التحديث" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateValueDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Sell Asset Dialog
    if (showSellAssetDialog) {
        var salePriceStr by remember { mutableStateOf(asset.currentValue.toString()) }
        val sarAccounts = accounts.filter { it.currency.equals("SAR", ignoreCase = true) }
        var selectedDestAccount by remember { mutableStateOf<Account?>(sarAccounts.firstOrNull()) }
        var saleNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSellAssetDialog = false },
            title = { Text(if (isArabic) "بيع / تصفية الأصل" else "Sell / Liquidate Asset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "تسجيل بيع الأصل وإيداع المبلغ في حسابك بالريال السعودي:" else "Record asset sale and deposit funds into SAR account:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = salePriceStr,
                        onValueChange = { salePriceStr = it },
                        label = { Text(if (isArabic) "سعر البيع النهائي (SAR)" else "Final Sale Price (SAR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isArabic) "حساب إيداع المبلغ:" else "Destination Account:",
                        style = MaterialTheme.typography.labelSmall
                    )

                    sarAccounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDestAccount = acc }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDestAccount?.id == acc.id,
                                onClick = { selectedDestAccount = acc }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${acc.name} (${String.format(Locale.US, "%,.2f", acc.balance)} SAR)")
                        }
                    }

                    OutlinedTextField(
                        value = saleNotes,
                        onValueChange = { saleNotes = it },
                        label = { Text(if (isArabic) "ملاحظات البيع" else "Sale Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = salePriceStr.toDoubleOrNull() ?: asset.currentValue
                        if (selectedDestAccount != null) {
                            viewModel.sellAsset(asset, price, selectedDestAccount!!.id, saleNotes)
                            showSellAssetDialog = false
                        }
                    },
                    enabled = selectedDestAccount != null && salePriceStr.isNotBlank()
                ) {
                    Text(if (isArabic) "تأكيد البيع" else "Confirm Sale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSellAssetDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (isArabic) "حذف الأصل" else "Delete Asset") },
            text = { Text(if (isArabic) "هل أنت تأكد من رغبتك في حذف هذا الأصل من السجلات؟" else "Are you sure you want to delete this asset record?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(asset)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedExpense)
                ) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

private data class CategorySummaryStats(
    val currentValue: Double,
    val purchaseCost: Double,
    val profit: Double,
    val profitPct: Double
)
