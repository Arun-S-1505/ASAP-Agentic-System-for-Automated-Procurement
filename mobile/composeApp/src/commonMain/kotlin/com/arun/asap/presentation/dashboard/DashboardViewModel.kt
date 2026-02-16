package com.arun.asap.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arun.asap.core.result.ApiResult
import com.arun.asap.data.model.AnalyticsSummaryDto
import com.arun.asap.data.model.ApprovalDecisionDto
import com.arun.asap.domain.usecase.ApproveDecisionUseCase
import com.arun.asap.domain.usecase.BatchApproveUseCase
import com.arun.asap.domain.usecase.BatchRejectUseCase
import com.arun.asap.domain.usecase.DetectUseCase
import com.arun.asap.domain.usecase.GetAnalyticsUseCase
import com.arun.asap.domain.usecase.GetDecisionsUseCase
import com.arun.asap.domain.usecase.RejectDecisionUseCase
import com.arun.asap.domain.usecase.UndoDecisionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 * Manages the state and business logic for approval decisions.
 */
class DashboardViewModel(
    private val detectUseCase: DetectUseCase,
    private val getDecisionsUseCase: GetDecisionsUseCase,
    private val undoDecisionUseCase: UndoDecisionUseCase,
    private val approveDecisionUseCase: ApproveDecisionUseCase,
    private val rejectDecisionUseCase: RejectDecisionUseCase,
    private val getAnalyticsUseCase: GetAnalyticsUseCase,
    private val batchApproveUseCase: BatchApproveUseCase,
    private val batchRejectUseCase: BatchRejectUseCase
) : ViewModel() {

    // Mutable state flows for internal updates
    private val _decisions = MutableStateFlow<List<ApprovalDecisionDto>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _analytics = MutableStateFlow<AnalyticsSummaryDto?>(null)
    private val _analyticsLoading = MutableStateFlow(false)

    // Public immutable state flows for UI observation
    val decisions: StateFlow<List<ApprovalDecisionDto>> = _decisions.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()
    val analytics: StateFlow<AnalyticsSummaryDto?> = _analytics.asStateFlow()
    val analyticsLoading: StateFlow<Boolean> = _analyticsLoading.asStateFlow()

    /**
     * Loads approval decisions from the repository.
     * Updates the decisions list or error message based on the result.
     */
    fun loadDecisions() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = getDecisionsUseCase()) {
                is ApiResult.Success -> {
                    _decisions.value = result.data.decisions
                    _errorMessage.value = null
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                }
                is ApiResult.Loading -> {
                    // Loading state already set
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Detects staged requisitions.
     * Updates the error message if detection fails, and reloads decisions on success.
     */
    fun detect() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = detectUseCase()) {
                is ApiResult.Success -> {
                    _errorMessage.value = null
                    // Reload decisions after successful detection
                    loadDecisions()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is ApiResult.Loading -> {
                    // Loading state already set
                }
            }
        }
    }

    /**
     * Undoes a decision for a specific requisition.
     * Reloads the decisions list on success.
     *
     * @param id The requisition ID to undo
     */
    fun undo(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = undoDecisionUseCase(id)) {
                is ApiResult.Success -> {
                    _errorMessage.value = null
                    _successMessage.value = "Decision cancelled successfully"
                    // Reload decisions after successful undo
                    loadDecisions()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is ApiResult.Loading -> {
                    // Loading state already set
                }
            }
        }
    }

    /**
     * Approves a pending requisition.
     * Reloads the decisions list on success.
     *
     * @param id The ERP requisition ID to approve
     * @param comment Optional approval comment
     */
    fun approve(id: String, comment: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val result = approveDecisionUseCase(id, comment)) {
                is ApiResult.Success -> {
                    _errorMessage.value = null
                    _successMessage.value = result.data.message
                    // Reload decisions after successful approval
                    loadDecisions()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is ApiResult.Loading -> {
                    // Loading state already set
                }
            }
        }
    }

    /**
     * Rejects a pending requisition.
     * Reloads the decisions list on success.
     *
     * @param id The ERP requisition ID to reject
     * @param comment Optional rejection reason
     */
    fun reject(id: String, comment: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val result = rejectDecisionUseCase(id, comment)) {
                is ApiResult.Success -> {
                    _errorMessage.value = null
                    _successMessage.value = result.data.message
                    // Reload decisions after successful rejection
                    loadDecisions()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is ApiResult.Loading -> {
                    // Loading state already set
                }
            }
        }
    }

    /**
     * Clears the current error message.
     * Useful for dismissing error notifications in the UI.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clears the current success message.
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    // ------------------------------------------------------------------
    // Selection (for batch operations)
    // ------------------------------------------------------------------

    /** Toggle selection of a decision by its ERP requisition ID. */
    fun toggleSelect(erpId: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(erpId)) remove(erpId) else add(erpId)
        }
    }

    /** Clear all selections. */
    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    // ------------------------------------------------------------------
    // Batch approve / reject
    // ------------------------------------------------------------------

    /** Batch-approve all currently selected decisions. */
    fun batchApprove(comment: String? = null) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val result = batchApproveUseCase(ids, comment)) {
                is ApiResult.Success -> {
                    val r = result.data
                    _successMessage.value = "Batch approved: ${r.processed} processed, ${r.failed} failed"
                    _selectedIds.value = emptySet()
                    loadDecisions()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    /** Batch-reject all currently selected decisions. */
    fun batchReject(comment: String? = null) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            when (val result = batchRejectUseCase(ids, comment)) {
                is ApiResult.Success -> {
                    val r = result.data
                    _successMessage.value = "Batch rejected: ${r.processed} processed, ${r.failed} failed"
                    _selectedIds.value = emptySet()
                    loadDecisions()
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                    _isLoading.value = false
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    // ------------------------------------------------------------------
    // Analytics
    // ------------------------------------------------------------------

    /** Load aggregated analytics from the backend. */
    fun loadAnalytics() {
        viewModelScope.launch {
            _analyticsLoading.value = true
            when (val result = getAnalyticsUseCase()) {
                is ApiResult.Success -> {
                    _analytics.value = result.data
                }
                is ApiResult.Error -> {
                    _errorMessage.value = result.message
                }
                is ApiResult.Loading -> {}
            }
            _analyticsLoading.value = false
        }
    }
}
