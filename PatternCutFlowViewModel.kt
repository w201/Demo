package sk.drevari.optitimb.ui.patternCutFlow

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import sk.drevari.optitimb.OTApp
import sk.drevari.optitimb.repo.OTRepository
import sk.drevari.optitimb.repo.db.data.PatternWithLogAndSpecie
import sk.drevari.optitimb.repo.db.entity.Pattern
import sk.drevari.optitimb.utils.Visualization.getTotalCuts
import sk.drevari.optitimb.utils.Visualization.visualize
import javax.inject.Inject

/**
 * Created by Sergii Volchenko on 15.06.2020.
 * sergey@volchenko.com
 * made in Ukraine
 */
class PatternCutFlowViewModel(val app: Application, val patternId: Int) : AndroidViewModel(app) {

    @Inject
    lateinit var repo: OTRepository

    private val _image = MutableLiveData<Bitmap>()
    val image: LiveData<Bitmap> = _image

    private val _totalSteps = MutableLiveData<Int>()
    val totalSteps: LiveData<Int> = _totalSteps

    private val _currentStep = MutableLiveData<Int>()
    val currentStep: LiveData<Int> = _currentStep


    lateinit var pattern: Pattern

    init {
        (app as OTApp).appComponents.inject(this)

        viewModelScope.launch {
            pattern = repo.getPatternById(patternId)!!
            _image.postValue(pattern.visualize(app, 0))
            _totalSteps.postValue(pattern.getTotalCuts())
            _currentStep.postValue(0)
        }
    }

    fun nextCut(): Boolean {
        if (currentStep.value!! < totalSteps.value!!) {
            _currentStep.value = if (_currentStep.value == -1) _currentStep.value!! + 2 else _currentStep.value!! + 1
            _image.postValue(pattern.visualize(app, _currentStep.value))
        }
        return currentStep.value!! < totalSteps.value!!
    }

    fun previousCut() =
        if (currentStep.value!! > 0) {
            _currentStep.value = _currentStep.value!! - 1
            _image.postValue(pattern.visualize(app, _currentStep.value))
            true
        } else false

    override fun onCleared() {
        super.onCleared()
        _image.value?.recycle()
    }

}