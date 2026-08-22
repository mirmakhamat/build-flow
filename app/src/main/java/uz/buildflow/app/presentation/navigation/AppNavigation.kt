package uz.buildflow.app.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import uz.buildflow.app.core.theme.DeepBluePrimary
import uz.buildflow.app.core.theme.SurfaceLight
import uz.buildflow.app.core.util.DatabaseBackupHelper
import uz.buildflow.app.di.AppContainer
import uz.buildflow.app.presentation.expenses.ExpensesScreen
import uz.buildflow.app.presentation.expenses.ExpensesViewModel
import uz.buildflow.app.presentation.objects.ObjectDetailScreen
import uz.buildflow.app.presentation.objects.ObjectDetailViewModel
import uz.buildflow.app.presentation.objects.ObjectsScreen
import uz.buildflow.app.presentation.objects.ObjectsViewModel
import uz.buildflow.app.presentation.reports.ReportsScreen
import uz.buildflow.app.presentation.transactions.IncomesScreen
import uz.buildflow.app.presentation.transactions.IncomesViewModel
import uz.buildflow.app.presentation.workers.*

sealed class ObjectTab(val tabName: String, val title: String, val icon: ImageVector) {
    object Dashboard : ObjectTab("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Workers : ObjectTab("workers", "Ishchilar", Icons.Default.Group)
    object Expenses : ObjectTab("expenses", "Xarajatlar", Icons.AutoMirrored.Filled.ReceiptLong)
    object Reports : ObjectTab("reports", "Hisobot", Icons.Default.Assessment)
}

@Composable
fun AppNavigation(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Obyekt kontekstida ekanligimizni aniqlash
    val currentObjectId = remember(currentRoute, navBackStackEntry) {
        navBackStackEntry?.arguments?.getString("objectId")
    }

    val isInsideObjectScope = currentObjectId != null && (
        currentRoute?.startsWith("object_dashboard/") == true ||
        currentRoute?.startsWith("object_workers/") == true ||
        currentRoute?.startsWith("object_expenses/") == true ||
        currentRoute?.startsWith("object_reports/") == true
    )

    val objectTabs = listOf(
        ObjectTab.Dashboard,
        ObjectTab.Workers,
        ObjectTab.Expenses,
        ObjectTab.Reports
    )

    Scaffold(
        bottomBar = {
            if (isInsideObjectScope && currentObjectId != null) {
                NavigationBar(containerColor = SurfaceLight) {
                    objectTabs.forEach { tab ->
                        val targetRoute = "object_${tab.tabName}/$currentObjectId"
                        val isSelected = currentRoute?.startsWith("object_${tab.tabName}") == true

                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DeepBluePrimary,
                                selectedTextColor = DeepBluePrimary,
                                indicatorColor = SurfaceLight
                            ),
                            onClick = {
                                if (currentRoute != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo("object_dashboard/$currentObjectId") {
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "objects",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. BARCHA OBYEKTLAR RO'YXATI (Bosh sahifa)
            composable("objects") {
                val viewModel: ObjectsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ObjectsViewModel.provideFactory(
                        container.objectRepository,
                        container.getObjectFinancialSummaryUseCase
                    )
                )
                ObjectsScreen(
                    viewModel = viewModel,
                    onObjectClick = { objId ->
                        navController.navigate("object_dashboard/$objId")
                    },
                    onExportDatabase = {
                        DatabaseBackupHelper.exportDatabase(context, container.database)
                    }
                )
            }

            // 2. OBYEKT DASHBOARD (Tanlangan Obyekt Tafsilotlari)
            composable("object_dashboard/{objectId}") { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: return@composable
                val viewModel: ObjectDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ObjectDetailViewModel.provideFactory(
                        objectId,
                        container.objectRepository,
                        container.getObjectFinancialSummaryUseCase
                    )
                )
                ObjectDetailScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.navigate("objects") {
                            popUpTo("objects") { inclusive = true }
                        }
                    },
                    onNavigateToWorkers = { navController.navigate("object_workers/$objectId") },
                    onNavigateToExpenses = { navController.navigate("object_expenses/$objectId") },
                    onNavigateToIncomes = { navController.navigate("incomes/$objectId") },
                    onNavigateToDailyAttendance = { navController.navigate("batch_attendance/$objectId") }
                )
            }

            // 3. OBYEKT ISHCHILARI (Faqat shu tanlangan obyekt uchun)
            composable("object_workers/{objectId}") { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: return@composable
                val viewModel: WorkersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = WorkersViewModel.provideFactory(
                        container.workerRepository,
                        container.objectRepository,
                        container.getWorkerStatsUseCase,
                        objectId
                    )
                )
                WorkersScreen(
                    viewModel = viewModel,
                    onBackToObjects = {
                        navController.navigate("objects") {
                            popUpTo("objects") { inclusive = true }
                        }
                    },
                    onWorkerClick = { workerId ->
                        navController.navigate("worker_detail/$workerId?objectId=$objectId")
                    },
                    onBatchAttendanceClick = {
                        navController.navigate("batch_attendance/$objectId")
                    }
                )
            }

            // 4. OBYEKT XARAJATLARI (Faqat shu tanlangan obyekt uchun)
            composable("object_expenses/{objectId}") { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: return@composable
                val viewModel: ExpensesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ExpensesViewModel.provideFactory(
                        container.expenseRepository,
                        container.expenseCategoryRepository,
                        objectId
                    )
                )
                ExpensesScreen(
                    viewModel = viewModel,
                    onBackToObjects = {
                        navController.navigate("objects") {
                            popUpTo("objects") { inclusive = true }
                        }
                    }
                )
            }

            // 5. OBYEKT MOLIYAVIY HISOBOTI (Faqat shu tanlangan obyekt uchun)
            composable("object_reports/{objectId}") { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: return@composable
                val summaryState = produceState<uz.buildflow.app.domain.model.ObjectFinancialSummary?>(initialValue = null, objectId) {
                    container.getObjectFinancialSummaryUseCase(objectId).collect { value = it }
                }
                ReportsScreen(
                    summary = summaryState.value,
                    onBackToObjects = {
                        navController.navigate("objects") {
                            popUpTo("objects") { inclusive = true }
                        }
                    }
                )
            }

            // 6. KIRIMLAR (Mijoz to'lovlari)
            composable("incomes/{objectId}") { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: return@composable
                val viewModel: IncomesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = IncomesViewModel.provideFactory(
                        container.transactionRepository,
                        container.objectRepository,
                        objectId
                    )
                )
                IncomesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. ISHCHI PROFILI
            composable(
                route = "worker_detail/{workerId}?objectId={objectId}",
                arguments = listOf(
                    navArgument("workerId") { type = NavType.StringType },
                    navArgument("objectId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val workerId = backStackEntry.arguments?.getString("workerId") ?: return@composable
                val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
                val viewModel: WorkerDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = WorkerDetailViewModel.provideFactory(
                        workerId,
                        container.workerRepository,
                        container.workerDayRepository,
                        container.transactionRepository,
                        container.getWorkerStatsUseCase
                    )
                )
                WorkerDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPayments = { objId, workerName ->
                        val safeObjId = if (objId.isNotBlank()) objId else objectId
                        val encodedName = Uri.encode(workerName)
                        navController.navigate("worker_payments/$workerId?objectId=$safeObjId&name=$encodedName")
                    }
                )
            }

            // 8. ISHCHI TO'LOVLARI & AVANSLARI
            composable(
                route = "worker_payments/{workerId}?objectId={objectId}&name={name}",
                arguments = listOf(
                    navArgument("workerId") { type = NavType.StringType },
                    navArgument("objectId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("name") { type = NavType.StringType; defaultValue = "Ishchi" }
                )
            ) { backStackEntry ->
                val workerId = backStackEntry.arguments?.getString("workerId") ?: return@composable
                val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
                val workerName = backStackEntry.arguments?.getString("name") ?: "Ishchi"

                val viewModel: WorkerPaymentsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = WorkerPaymentsViewModel.provideFactory(
                        workerId,
                        objectId,
                        container.workerRepository,
                        container.transactionRepository,
                        container.getWorkerStatsUseCase
                    )
                )
                WorkerPaymentsScreen(
                    viewModel = viewModel,
                    workerName = workerName,
                    onBack = { navController.popBackStack() }
                )
            }

            // 9. GURUHLI DAVOMAT
            composable("batch_attendance/{objectId}") { backStackEntry ->
                val objectId = backStackEntry.arguments?.getString("objectId") ?: return@composable
                DailyAttendanceBatchScreen(
                    objectId = objectId,
                    workerRepository = container.workerRepository,
                    workerDayRepository = container.workerDayRepository,
                    transactionRepository = container.transactionRepository,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
