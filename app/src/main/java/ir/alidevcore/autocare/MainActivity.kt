package ir.alidevcore.autocare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import ir.alidevcore.autocare.ui.dashboard.DashboardScreen
import ir.alidevcore.autocare.ui.dashboard.DashboardViewModel
import ir.alidevcore.autocare.ui.expenses.ExpensesReportScreen
import ir.alidevcore.autocare.ui.expenses.ExpensesViewModel
import ir.alidevcore.autocare.ui.export.ExportScreen
import ir.alidevcore.autocare.ui.export.ExportViewModel
import ir.alidevcore.autocare.ui.items.ItemViewModel
import ir.alidevcore.autocare.ui.items.MaintenanceItemsScreen
import ir.alidevcore.autocare.ui.navigation.Screen
import ir.alidevcore.autocare.ui.plans.AddEditPlanScreen
import ir.alidevcore.autocare.ui.plans.PlanViewModel
import ir.alidevcore.autocare.ui.records.AddEditRecordScreen
import ir.alidevcore.autocare.ui.records.RecordViewModel
import ir.alidevcore.autocare.ui.records.RecordsHistoryScreen
import ir.alidevcore.autocare.ui.records.RecordsHistoryViewModel
import ir.alidevcore.autocare.ui.theme.AutoCareTheme
import ir.alidevcore.autocare.ui.vehicles.VehicleListScreen
import ir.alidevcore.autocare.ui.vehicles.VehicleViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val initialVehicleId = intent?.getLongExtra("vehicleId", 0L) ?: 0L

        setContent {
            AutoCareTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val startDestination = if (initialVehicleId > 0L) {
                            Screen.Dashboard.createRoute(initialVehicleId)
                        } else {
                            Screen.Vehicles.route
                        }

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            // 1. Vehicle Selection / List
                            composable(Screen.Vehicles.route) {
                                val viewModel: VehicleViewModel = hiltViewModel()
                                VehicleListScreen(
                                    viewModel = viewModel,
                                    onVehicleSelected = { vehicleId ->
                                        navController.navigate(Screen.Dashboard.createRoute(vehicleId))
                                    }
                                )
                            }

                            // 2. Vehicle Dashboard
                            composable(
                                route = Screen.Dashboard.route,
                                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
                                val viewModel: DashboardViewModel = hiltViewModel()
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToAddRecord = { itemId, planId ->
                                        navController.navigate(Screen.AddRecord.createRoute(vehicleId, itemId = itemId, planId = planId))
                                    },
                                    onNavigateToAddPlan = { itemId ->
                                        navController.navigate(Screen.AddPlan.createRoute(vehicleId, itemId = itemId))
                                    },
                                    onNavigateToHistory = {
                                        navController.navigate(Screen.RecordsHistory.createRoute(vehicleId))
                                    },
                                    onNavigateToItems = {
                                        navController.navigate(Screen.Items.createRoute(vehicleId))
                                    },
                                    onNavigateToExpenses = {
                                        navController.navigate(Screen.ExpensesReport.createRoute(vehicleId))
                                    },
                                    onNavigateToExport = {
                                        navController.navigate(Screen.ExportData.createRoute(vehicleId))
                                    }
                                )
                            }

                            // 3. Maintenance Items Screen
                            composable(
                                route = Screen.Items.route,
                                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
                                val viewModel: ItemViewModel = hiltViewModel()
                                MaintenanceItemsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToRecordService = { itemId ->
                                        navController.navigate(Screen.AddRecord.createRoute(vehicleId, itemId = itemId))
                                    },
                                    onNavigateToItemHistory = { itemId ->
                                        navController.navigate(Screen.RecordsHistory.createRoute(vehicleId, itemId = itemId))
                                    }
                                )
                            }

                            // 4. Add / Edit Record Screen
                            composable(
                                route = Screen.AddRecord.route,
                                arguments = listOf(
                                    navArgument("vehicleId") { type = NavType.LongType },
                                    navArgument("itemId") { type = NavType.LongType; defaultValue = -1L },
                                    navArgument("recordId") { type = NavType.LongType; defaultValue = -1L },
                                    navArgument("planId") { type = NavType.LongType; defaultValue = -1L }
                                )
                            ) {
                                val viewModel: RecordViewModel = hiltViewModel()
                                AddEditRecordScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // 5. Add / Edit Plan Screen
                            composable(
                                route = Screen.AddPlan.route,
                                arguments = listOf(
                                    navArgument("vehicleId") { type = NavType.LongType },
                                    navArgument("itemId") { type = NavType.LongType; defaultValue = -1L },
                                    navArgument("planId") { type = NavType.LongType; defaultValue = -1L }
                                )
                            ) {
                                val viewModel: PlanViewModel = hiltViewModel()
                                AddEditPlanScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // 6. Records History Screen
                            composable(
                                route = Screen.RecordsHistory.route,
                                arguments = listOf(
                                    navArgument("vehicleId") { type = NavType.LongType },
                                    navArgument("itemId") { type = NavType.LongType; defaultValue = -1L }
                                )
                            ) { backStackEntry ->
                                val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: 0L
                                val viewModel: RecordsHistoryViewModel = hiltViewModel()
                                RecordsHistoryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() },
                                    onEditRecord = { recId ->
                                        navController.navigate(Screen.AddRecord.createRoute(vehicleId, recordId = recId))
                                    },
                                    onAddNewRecord = {
                                        navController.navigate(Screen.AddRecord.createRoute(vehicleId))
                                    }
                                )
                            }

                            // 7. Expenses & Analytics Screen
                            composable(
                                route = Screen.ExpensesReport.route,
                                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
                            ) {
                                val viewModel: ExpensesViewModel = hiltViewModel()
                                ExpensesReportScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            // 8. Export Screen
                            composable(
                                route = Screen.ExportData.route,
                                arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
                            ) {
                                val viewModel: ExportViewModel = hiltViewModel()
                                ExportScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
