package sk.drevari.optitimb.ui.settings.inputData

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_setting_input_data.*
import sk.drevari.optitimb.R
import sk.drevari.optitimb.data.RemoveClick
import sk.drevari.optitimb.repo.db.entity.METRIC_HEARTWOOD

class InputDataSettingsFragment : Fragment() {


    lateinit var adapterHW: HeartwoodAdapter
    lateinit var adapterWEB: WEBAdapter

    private lateinit var viewModel: InputDataSettingsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(InputDataSettingsViewModel::class.java)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting_input_data, container, false)
    }
    private fun getRBByIndex(index: Int) = when (index) {
        1 -> rbBSDiameter
        2 -> rbSTDiameter
        3 -> rbMTDiameter
        else -> rbBTDiameter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        rangeHWR.tickBottomLabels =
            createTickLabel(rangeHWR.tickInterval.toInt(), rangeHWR.tickStart.toInt(), rangeHWR.tickEnd.toInt())
        rangeWEB.tickBottomLabels =
            createTickLabel(rangeWEB.tickInterval.toInt(), rangeWEB.tickStart.toInt(), rangeWEB.tickEnd.toInt())

        viewModel.centerInsertion.observe(viewLifecycleOwner) {
            tvLblHWRange.text = getString(if (it == METRIC_HEARTWOOD.DIAM) getLengthLabelAccordingMetric() else R.string.PERCENT)
        }

        viewModel.diamInsertion.observe(viewLifecycleOwner) {
            getRBByIndex(it).isChecked = true
        }

        setupHW()

        setupWEB()


        viewModel.heartWoodList.observe(viewLifecycleOwner) {
            adapterHW.setData(it as ArrayList)
        }

        viewModel.waneyEdgeBoard.observe(viewLifecycleOwner) {
            adapterWEB.setData(it as ArrayList)
        }


    }

    private fun setupHW() {
        adapterHW = HeartwoodAdapter(context!!)
        rvHWRange.layoutManager = LinearLayoutManager(context!!)
        rvHWRange.itemAnimator = DefaultItemAnimator()
        rvHWRange.adapter = adapterHW
        ibAddHWR.setOnClickListener(::onClick_ibAddHWR)
    }

    private fun setupWEB() {
        adapterWEB = WEBAdapter(context!!)
        rvWEBRange.layoutManager = LinearLayoutManager(context!!)
        rvWEBRange.itemAnimator = DefaultItemAnimator()
        rvWEBRange.adapter = adapterWEB
        ibAddWEB.setOnClickListener(::onClick_ibAddWEB)

    }

    override fun onStart() {
        super.onStart()
        adapterHW.listener = object: RemoveClick {
            override fun removeUUID(id: Int) {
                viewModel.removeHeartWood(id)
            }
        }

        adapterWEB.listener = object: RemoveClick {
            override fun removeUUID(id: Int) {
                viewModel.removeWEB(id)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        adapterHW.listener = null

    }

    fun onClick_ibAddHWR(view2: View) {
        viewModel.addHeartWood(rangeHWR.leftPinValue.toInt(), rangeHWR.rightPinValue.toInt())
    }

    fun onClick_ibAddWEB(view2: View) {
        viewModel.addWEBoard(rangeWEB.leftPinValue.toInt(), rangeWEB.rightPinValue.toInt())
    }

    private fun createTickLabel(interval: Int, start: Int, end: Int): Array<String> {
        val size = ((end - start) / interval) + 1
        val labels = Array(size) { it.toString() }
        var inx = 0
        for (i in start..end step interval) {
            labels[inx++] = "$i"
        }
        return labels
    }

    private fun getLengthLabelAccordingMetric(): Int {
        return if (viewModel.isSi) R.string.cm else R.string.inch
    }
}
