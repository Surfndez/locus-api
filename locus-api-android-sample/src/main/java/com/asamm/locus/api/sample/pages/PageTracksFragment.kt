/**
 * Created by menion on 29/08/2016.
 * This code is part of Locus project from Asamm Software, s. r. o.
 */
package com.asamm.locus.api.sample.pages

import com.asamm.locus.api.sample.utils.BasicAdapterItem
import com.asamm.locus.api.sample.utils.SampleCalls
import locus.api.android.ActionTools
import locus.api.android.utils.LocusUtils
import java.util.*

class PageTracksFragment : ABasePageFragment() {

    override val items: List<BasicAdapterItem>
        get() {
            // create list of items
            val items = ArrayList<BasicAdapterItem>()
            items.add(BasicAdapterItem(1,
                    "Display one track"))
            items.add(BasicAdapterItem(2,
                    "Display multiple tracks"))

            items.add(BasicAdapterItem(8,
                    "Start Navigation (to a point)",
                    "Allows to start Navigation to a certain point. This function is a direct intent so it starts Locus if it's not running. You may also use 'actionStartGuiding' to start Guiding instead of Navigation."))
            items.add(BasicAdapterItem(9,
                    "Start Navigation (to an Address)",
                    "Allows to start Navigation to a certain point defined by an address."))

            items.add(BasicAdapterItem(20,
                    "Start Track recording",
                    "Allows to start track recording. This call is sent as Broadcast event so it requires to know LocusVersion that should start track recording."))
            items.add(BasicAdapterItem(21,
                    "Stop Track recording",
                    "The same as previous sample, just used to stop the track recording."))
            return items
        }

    @Throws(Exception::class)
    override fun onItemClicked(itemId: Int, activeLocus: LocusUtils.LocusVersion) {
        when (itemId) {
            1 -> SampleCalls.callSendOneTrack(activity!!)
            2 -> SampleCalls.callSendMultipleTracks(activity!!)
            8 -> ActionTools.actionStartNavigation(activity,
                    SampleCalls.generateWaypoint(1))
            9 -> ActionTools.actionStartNavigation(activity,
                    "Řipská 20, Praha 2, ČR")
            20 -> // start track recording. Recording profile "Cycle" is optional parameter. If
                // this parameter is not used, last used profile is used for recording.
                ActionTools.actionTrackRecordStart(activity, activeLocus, "Car")
            21 -> ActionTools.actionTrackRecordStop(activity, activeLocus, true)
        }
    }
}