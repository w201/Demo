package sk.drevari.optitimb.repo.db.data

import android.content.Context
import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Junction
import androidx.room.Relation
import sk.drevari.optitimb.OTApp
import sk.drevari.optitimb.R
import sk.drevari.optitimb.data.Dic
import sk.drevari.optitimb.data.OTSetting
import sk.drevari.optitimb.repo.db.entity.*
import java.text.DecimalFormat

/**
 * Created by Sergii Volchenko on 25.02.2020.
 * sergey@volchenko.com
 * made in Ukraine
 */
data class MachineWithPLC(
    @Embedded var machine: Machine,
    @Relation(
        parentColumn = "machineId",
        entityColumn = "plcId",
        associateBy = Junction(MachineAndPLC::class)
    )
    var plc: List<PLC>? = null
) {
    @Ignore
    private val diamFormat = DecimalFormat("#.#").apply {
        maximumFractionDigits = 2
        minimumIntegerDigits = 1
        minimumFractionDigits = 0
    }

    private fun getValueWithUnitsLength(context: Context, value: Float) =
        "${diamFormat.format(value)} ${context.getString(if (OTSetting.isSi) R.string.mm else R.string.inch)}"

    private fun getValueWithUnitsLength(context: Context, value: Int) =
        "$value ${context.getString(if (OTSetting.isSi) R.string.mm else R.string.inch)}"

    fun convertToDic(app: OTApp): ArrayList<Dic<String, String>> {
        val list = ArrayList<Dic<String, String>>()
       /* list.add(
            Dic(
                app.getString(R.string.TYPE),
                app.getString(machine.getTypeResourseId())
            )
        )*/
        list.add(
            Dic(
                app.getString(R.string.MACHINE_PAR2),
                getValueWithUnitsLength(app, machine.horizontalDiam ?: 0f)
            )
        )
        list.add(
            Dic(
                app.getString(R.string.MACHINE_PAR3),
                getValueWithUnitsLength(app, machine.horizontalKerf ?: 0f)
            )
        )

        list.add(
            Dic(
                app.getString(R.string.MACHINE_PAR4),
                getValueWithUnitsLength(app, machine.verticalDiam ?: 0f)
            )
        )
        list.add(
            Dic(
                app.getString(R.string.MACHINE_PAR5),
                getValueWithUnitsLength(app, machine.verticalKerf ?: 0f)
            )
        )

        if (machine.horizontalCut != null) {
            list.add(
                Dic(
                    app.getString(R.string.MACHINE_PAR9),
                    getValueWithUnitsLength(app, machine.horizontalCut!!)
                )
            )
        }

        machine.verticalCut?.let {
            list.add(
                Dic(
                    app.getString(R.string.MACHINE_PAR8),
                    getValueWithUnitsLength(app, it)
                )
            )
        }
        machine.cutPlaneMax?.let {
            list.add(
                Dic(
                    app.getString(R.string.MACHINE_PAR10),
                    getValueWithUnitsLength(app, it)
                )
            )
        }
        machine.cutPlaneMin?.let {
            list.add(
                Dic(
                    app.getString(R.string.MACHINE_PAR11),
                    getValueWithUnitsLength(app, it)
                )
            )
        }
        machine.thickLastboard?.let {
            list.add(
                Dic(
                    app.getString(R.string.MACHINE_PAR12),
                    getValueWithUnitsLength(app, it)
                )
            )
        }
        machine.logLifting?.let {
            list.add(
                Dic(
                    app.getString(R.string.MACHINE_PAR13),
                    getValueWithUnitsLength(app, it)
                )
            )
        }

        plc?.let {
            it.forEach {plc ->
//                list.add(Dic(null, null))
                list.addAll(plc.paramsToDic(app))
            }
        }

        return list
    }

}