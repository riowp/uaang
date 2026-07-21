package com.rio.keuanganku

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rio.keuanganku.security.SecurityManager
import com.rio.keuanganku.ui.AnalysisScreen
import com.rio.keuanganku.ui.AssetScreen
import com.rio.keuanganku.ui.BackupScreen
import com.rio.keuanganku.ui.BudgetScreen
import com.rio.keuanganku.ui.CategoryScreen
import com.rio.keuanganku.ui.DashboardScreen
import com.rio.keuanganku.ui.DebtScreen
import com.rio.keuanganku.ui.HistoryScreen
import com.rio.keuanganku.ui.KeuanganKuTheme
import com.rio.keuanganku.ui.LockScreen
import com.rio.keuanganku.ui.MoreScreen
import com.rio.keuanganku.ui.PlanScreen
import com.rio.keuanganku.ui.SecurityScreen
import com.rio.keuanganku.ui.TransactionScreen
import com.rio.keuanganku.work.MonthlyReportWorker
import java.util.concurrent.TimeUnit

// FragmentActivity dibutuhkan oleh BiometricPrompt (sidik jari)
class MainActivity : FragmentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Pelaporan otomatis: cek harian, buat PDF setiap tanggal 1
        val req = PeriodicWorkRequestBuilder<MonthlyReportWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "monthly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )

        setContent {
            val vm: FinanceViewModel = viewModel()
            val dark by vm.darkMode.collectAsState()
            val needLock = remember { SecurityManager.isPasscodeSet(this) }
            var unlocked by rememberSaveable { mutableStateOf(!needLock) }

            KeuanganKuTheme(dark = dark) {
                if (!unlocked) {
                    LockScreen(onUnlocked = { unlocked = true })
                } else {
                    AppRoot(vm)
                }
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Route yang berada di bawah menu "Lainnya" */
private val MORE_ROUTES = setOf("more", "asset", "debt", "category", "security", "plan", "backup", "history")

@Composable
fun AppRoot(vm: FinanceViewModel) {
    val nav = rememberNavController()

    val items = listOf(
        NavItem("dashboard", "Beranda", Icons.Default.Home),
        NavItem("txn", "Transaksi", Icons.Default.List),
        NavItem("analysis", "Analisis", Icons.Default.DateRange),
        NavItem("budget", "Anggaran", Icons.Default.ShoppingCart),
        NavItem("more", "Lainnya", Icons.Default.Menu)
    )

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = if (item.route == "more") {
                        currentRoute in MORE_ROUTES
                    } else {
                        currentRoute == item.route
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(item.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, item.label) },
                        label = {
                            Text(
                                item.label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "dashboard",
            modifier = Modifier.padding(pad)
        ) {
            composable("dashboard") { DashboardScreen(vm) }
            composable("txn") { TransactionScreen(vm) }
            composable("analysis") { AnalysisScreen(vm) }
            composable("budget") { BudgetScreen(vm) }
            composable("more") { MoreScreen(nav) }
            composable("asset") { AssetScreen(vm) }
            composable("plan") { PlanScreen(vm) }
            composable("debt") { DebtScreen(vm) }
            composable("category") { CategoryScreen(vm) }
            composable("security") { SecurityScreen() }
            composable("backup") { BackupScreen() }
            composable("history") { HistoryScreen(vm) }
        }
    }
}
