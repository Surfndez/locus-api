package locus.api.android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.android.utils.LocusConst;
import locus.api.android.utils.LocusInfo;
import locus.api.android.utils.LocusUtils;
import locus.api.android.utils.LocusUtils.LocusVersion;
import locus.api.android.utils.LocusUtils.VersionCode;
import locus.api.android.utils.UtilsAnd;
import locus.api.android.utils.exceptions.RequiredVersionMissingException;
import locus.api.objects.Storable;
import locus.api.objects.extra.GeoDataExtra;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.Point;
import locus.api.objects.extra.Track;
import locus.api.utils.DataReaderBigEndian;
import locus.api.utils.DataWriterBigEndian;
import locus.api.utils.Logger;

@SuppressWarnings({"unused", "WeakerAccess", "DeprecatedIsStillUsed"})
public class ActionTools {

    // tag for logger
    private static final String TAG = "ActionTools";

    //*************************************************
    // PART ALREADY REWROTE IN 'ACTIONBASICS' CLASS
    //*************************************************

    @Deprecated // use ActionBasic.getLocusInfo() instead
    public static String getLocusRootDirectory(Context context)
            throws RequiredVersionMissingException {
        LocusInfo locusInfo = getLocusInfoData(context);
        if (locusInfo != null) {
            return locusInfo.getRootDirectory();
        } else {
            return null;
        }
    }

    @Deprecated // use ActionBasic.getLocusInfo() instead
    public static boolean isPeriodicUpdatesEnabled(Context context)
            throws RequiredVersionMissingException {
        LocusInfo locusInfo = getLocusInfoData(context);
        return locusInfo != null &&
                locusInfo.isPeriodicUpdatesEnabled();
    }

    private static LocusInfo getLocusInfoData(Context ctx)
            throws RequiredVersionMissingException {
        return getLocusInfo(ctx, LocusUtils.createLocusVersion(ctx));
    }

    @Deprecated // use ActionBasic.getLocusInfo() instead
    public static LocusInfo getLocusInfo(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        // get scheme if valid Locus is available
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_01,
                LocusConst.CONTENT_PROVIDER_PATH_INFO);

        // get data
        Cursor cursor = null;
        try {
            cursor = queryData(ctx, scheme, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            // return info container
            return LocusInfo.create(cursor);
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getLocusInfo(" + ctx + ", " + lv + ")", e);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }
        return null;
    }

    @Deprecated // use ActionBasic.getLocusInfo() instead
    public static LocusInfo getDataLocusInfo(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        // get scheme if valid Locus is available
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_13,
                LocusConst.CONTENT_PROVIDER_PATH_DATA + "/" + LocusConst.VALUE_LOCUS_INFO);

        // execute action
        Cursor cursor = null;
        try {
            byte[] data = queryData(ctx, scheme, null, LocusConst.VALUE_LOCUS_INFO);
            if (data != null && data.length > 0) {
                LocusInfo li = new LocusInfo();
                li.read(data);
                return li;
            }
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getDataLocusInfo(" + ctx + ", " + lv + ")", e);
        }
        return null;
    }

    @Deprecated // use ActionBasic.getUpdateContainer() instead
    public static UpdateContainer getDataUpdateContainer(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        // get scheme if valid Locus is available
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_13,
                LocusConst.CONTENT_PROVIDER_PATH_DATA + "/" + LocusConst.VALUE_UPDATE_CONTAINER);

        // execute action
        Cursor cursor = null;
        try {
            byte[] data = queryData(ctx, scheme, null, LocusConst.VALUE_UPDATE_CONTAINER);
            if (data != null && data.length > 0) {
                UpdateContainer uc = new UpdateContainer();
                uc.read(data);
                return uc;
            }
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getDataUpdateContainer(" + ctx + ", " + lv + ")", e);
        }
        return null;
    }

    //*************************************************
    // NAVIGATION
    //*************************************************

    /**
     * Intent that starts navigation in Locus app based on defined target.
     *
     * @param act       current activity
     * @param name      name of target
     * @param latitude  latitude of target
     * @param longitude longitude of target
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static void actionStartNavigation(Activity act,
            String name, double latitude, double longitude)
            throws RequiredVersionMissingException {
        // check required version
        if (!LocusUtils.isLocusAvailable(act, VersionCode.UPDATE_01)) {
            throw new RequiredVersionMissingException(VersionCode.UPDATE_01);
        }

        // call Locus
        Intent intent = new Intent(LocusConst.ACTION_NAVIGATION_START);
        if (name != null) {
            intent.putExtra(LocusConst.INTENT_EXTRA_NAME, name);
        }
        intent.putExtra(LocusConst.INTENT_EXTRA_LATITUDE, latitude);
        intent.putExtra(LocusConst.INTENT_EXTRA_LONGITUDE, longitude);
        act.startActivity(intent);
    }

    /**
     * Intent that starts navigation in Locus app based on defined target.
     *
     * @param act current activity
     * @param pt  waypoint - destination
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static void actionStartNavigation(Activity act, Point pt)
            throws RequiredVersionMissingException {
        // check required version
        if (!LocusUtils.isLocusAvailable(act, VersionCode.UPDATE_01)) {
            throw new RequiredVersionMissingException(VersionCode.UPDATE_01);
        }

        // call Locus
        Intent intent = new Intent(LocusConst.ACTION_NAVIGATION_START);
        LocusUtils.addWaypointToIntent(intent, pt);
        act.startActivity(intent);
    }

    /**
     * Intent that starts navigation in Locus to target address.
     *
     * @param act     current activity
     * @param address target address
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static void actionStartNavigation(Activity act, String address)
            throws RequiredVersionMissingException {
        // check required version
        if (!LocusUtils.isLocusAvailable(act, VersionCode.UPDATE_08)) {
            throw new RequiredVersionMissingException(VersionCode.UPDATE_08);
        }

        // call Locus
        Intent intent = new Intent(LocusConst.ACTION_NAVIGATION_START);
        intent.putExtra(LocusConst.INTENT_EXTRA_ADDRESS_TEXT, address);
        act.startActivity(intent);
    }

    //*************************************************
    // GUIDING
    //*************************************************

    public static void actionStartGuiding(Activity act,
            String name, double latitude, double longitude)
            throws RequiredVersionMissingException {
        if (LocusUtils.isLocusAvailable(act, 243, 243, 0)) {
            Intent intent = new Intent(LocusConst.ACTION_GUIDING_START);
            if (name != null) {
                intent.putExtra(LocusConst.INTENT_EXTRA_NAME, name);
            }
            intent.putExtra(LocusConst.INTENT_EXTRA_LATITUDE, latitude);
            intent.putExtra(LocusConst.INTENT_EXTRA_LONGITUDE, longitude);
            act.startActivity(intent);
        } else {
            throw new RequiredVersionMissingException(243);
        }
    }

    public static void actionStartGuiding(Activity act, Point pt)
            throws RequiredVersionMissingException {
        if (LocusUtils.isLocusAvailable(act, 243, 243, 0)) {
            Intent intent = new Intent(LocusConst.ACTION_GUIDING_START);
            LocusUtils.addWaypointToIntent(intent, pt);
            act.startActivity(intent);
        } else {
            throw new RequiredVersionMissingException(243);
        }
    }

    //*************************************************
    // POINTS HANDLING
    //*************************************************

    @Deprecated // use ActionBasics.getPoint instead
    public static Point getLocusWaypoint(Context ctx, LocusVersion lv, long ptId)
            throws RequiredVersionMissingException {
        // check version
        int minVersion = VersionCode.UPDATE_01.vcFree;
        if (!LocusUtils.isLocusFreePro(lv, minVersion)) {
            throw new RequiredVersionMissingException(minVersion);
        }

        // generate cursor
        Cursor cursor;
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_01,
                LocusConst.CONTENT_PROVIDER_PATH_WAYPOINT);
        scheme = ContentUris.withAppendedId(scheme, ptId);
        cursor = ctx.getContentResolver().query(scheme,
                null, null, null, null);

        // check cursor
        if (cursor == null || !cursor.moveToFirst()) {
            Logger.INSTANCE.logW(TAG, "getLocusWaypoint(" + ctx + ", " + ptId + "), " +
                    "'cursor' in not valid");
            return null;
        }

        // handle result
        try {
            Point pt = new Point();
            pt.read(cursor.getBlob(1));
            return pt;
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getLocusWaypoint(" + ctx + ", " + ptId + ")", e);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }
        return null;
    }

    @Deprecated // use ActionBasic.getPointsId instead
    public static long[] getLocusWaypointId(Context ctx, LocusVersion lv, String ptName)
            throws RequiredVersionMissingException {
        // check version (available only in Free/Pro)
        int minVersion = VersionCode.UPDATE_03.vcFree;
        if (!LocusUtils.isLocusFreePro(lv, minVersion)) {
            throw new RequiredVersionMissingException(minVersion);
        }

        // generate cursor
        Cursor cursor;
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_03,
                LocusConst.CONTENT_PROVIDER_PATH_WAYPOINT);
        cursor = ctx.getContentResolver().query(scheme,
                null, "getWaypointId", new String[]{ptName}, null);

        // handle result
        long[] result = null;
        try {
            result = new long[cursor.getCount()];
            for (int i = 0, m = result.length; i < m; i++) {
                cursor.moveToPosition(i);
                result[i] = cursor.getLong(0);
            }
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getLocusWaypointId(" + ctx + ", " + ptName + ")", e);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }
        return result;
    }

    @Deprecated // use ActionBasics.updatePoint instead
    public static int updateLocusWaypoint(Context context, LocusVersion lv,
            Point wpt, boolean forceOverwrite)
            throws RequiredVersionMissingException {
        return updateLocusWaypoint(context, lv, wpt, forceOverwrite, false);
    }

    @Deprecated // use ActionBasic.updatePoint instead
    public static int updateLocusWaypoint(Context ctx, LocusVersion lv,
            Point wpt, boolean forceOverwrite, boolean loadAllGcWaypoints)
            throws RequiredVersionMissingException {
        // check version (available only in Free/Pro)
        int minVersion = VersionCode.UPDATE_01.vcFree;
        if (!LocusUtils.isLocusFreePro(lv, minVersion)) {
            throw new RequiredVersionMissingException(minVersion);
        }

        // generate cursor
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_01,
                LocusConst.CONTENT_PROVIDER_PATH_WAYPOINT);

        // define empty cursor
        ContentValues cv = new ContentValues();
        cv.put("waypoint", wpt.getAsBytes());
        cv.put("forceOverwrite", forceOverwrite);
        cv.put("loadAllGcWaypoints", loadAllGcWaypoints);
        return ctx.getContentResolver().update(scheme, cv, null, null);
    }

    @Deprecated // use ActionBasic.updatePoint instead
    public static void displayWaypointScreen(Context ctx, LocusVersion lv, long wptId)
            throws RequiredVersionMissingException {
        displayWaypointScreen(ctx, lv, wptId, "");
    }

    @Deprecated // use ActionBasic.updatePoint instead
    public static void displayWaypointScreen(Context ctx, LocusVersion lv, long wptId,
            String packageName, String className, String returnDataName, String returnDataValue)
            throws RequiredVersionMissingException {
        // prepare callback
        String callback = GeoDataExtra.Companion.generateCallbackString(
                "", packageName, className, returnDataName, returnDataValue);

        // call intent
        displayWaypointScreen(ctx, lv, wptId, callback);
    }

    @Deprecated // use ActionBasic.updatePoint instead
    private static void displayWaypointScreen(Context ctx, LocusVersion lv, long wptId, String callback)
            throws RequiredVersionMissingException {
        // check version (available only in Free/Pro)
        if (!LocusUtils.isLocusFreePro(lv, VersionCode.UPDATE_07.vcFree)) {
            throw new RequiredVersionMissingException(VersionCode.UPDATE_07);
        }

        // call intent
        Intent intent = new Intent(LocusConst.ACTION_DISPLAY_POINT_SCREEN);
        intent.putExtra(LocusConst.INTENT_EXTRA_ITEM_ID, wptId);
        if (callback != null && callback.length() > 0) {
            intent.putExtra(Point.TAG_EXTRA_CALLBACK, callback);
        }
        ctx.startActivity(intent);
    }

    //*************************************************
    // TRACKS HANDLING
    //*************************************************

    /**
     * Get full track from Locus database with all possible information, like
     * {@link GeoDataExtra} object
     *
     * @param ctx     current context
     * @param trackId unique ID of track in Locus database
     * @return {@link locus.api.objects.extra.Track} or <i>null</i> in case of problem
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasic.getTrack instead
    public static Track getLocusTrack(Context ctx, LocusVersion lv, long trackId)
            throws RequiredVersionMissingException {
        // check version
        int minVersion = VersionCode.UPDATE_10.vcFree;
        if (!LocusUtils.isLocusFreePro(lv, minVersion)) {
            throw new RequiredVersionMissingException(minVersion);
        }

        // generate cursor
        Cursor cursor;
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_10,
                LocusConst.CONTENT_PROVIDER_PATH_TRACK);
        scheme = ContentUris.withAppendedId(scheme, trackId);
        cursor = ctx.getContentResolver().query(scheme,
                null, null, null, null);

        // check cursor
        if (cursor == null || !cursor.moveToFirst()) {
            Logger.INSTANCE.logW(TAG, "getLocusTrack(" + ctx + ", " + trackId + "), " +
                    "'cursor' in not valid");
            return null;
        }

        // handle result
        try {
            Track track = new Track();
            track.read(cursor.getBlob(1));
            return track;
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getLocusTrack(" + ctx + ", " + trackId + ")", e);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }
        return null;
    }

    //*************************************************
    // LOCATION
    //*************************************************

    /**
     * Start basic "Pick location" event.
     *
     * @param act current activity
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionPickLocation instead
    public static void actionPickLocation(Activity act)
            throws RequiredVersionMissingException {
        if (LocusUtils.isLocusAvailable(act, 235, 235, 0)) {
            Intent intent = new Intent(LocusConst.ACTION_PICK_LOCATION);
            act.startActivity(intent);
        } else {
            throw new RequiredVersionMissingException(235);
        }
    }

    //*************************************************
    // FILE PICKER
    //*************************************************

    /**
     * Allow to call activity for File pick. You can use Locus picker for this purpose, but
     * check if Locus version 231 and above are installed <b>isLocusAvailable(context, 231)</b>!
     *
     * @param activity    starting activity that also receive result
     * @param requestCode request code
     * @throws ActivityNotFoundException thrown in case of missing required Locus app
     */
    @Deprecated // use ActionFiles.actionPickFile instead
    public static void actionPickFile(Activity activity, int requestCode)
            throws ActivityNotFoundException {
        intentPick("org.openintents.action.PICK_FILE",
                activity, requestCode, null, null);
    }

    @Deprecated // use ActionFiles.actionPickFile instead
    public static void actionPickFile(Activity activity, int requestCode,
            String title, String[] filter) throws ActivityNotFoundException {
        intentPick("org.openintents.action.PICK_FILE",
                activity, requestCode, title, filter);
    }

    @Deprecated // use ActionFiles.actionPickDir instead
    public static void actionPickDir(Activity activity, int requestCode)
            throws ActivityNotFoundException {
        intentPick("org.openintents.action.PICK_DIRECTORY",
                activity, requestCode, null, null);
    }

    @Deprecated // use ActionFiles.actionPickDir instead
    public static void actionPickDir(Activity activity, int requestCode,
            String title) throws ActivityNotFoundException {
        intentPick("org.openintents.action.PICK_DIRECTORY",
                activity, requestCode, title, null);
    }

    @Deprecated
    private static void intentPick(String action, Activity activity, int requestCode,
            String title, String[] filter) {
        // create intent
        Intent intent = new Intent(action);
        if (title != null && title.length() > 0) {
            intent.putExtra("org.openintents.extra.TITLE", title);
        }
        if (filter != null && filter.length > 0) {
            intent.putExtra("org.openintents.extra.FILTER", filter);
        }

        // execute request
        activity.startActivityForResult(intent, requestCode);
    }

    //*************************************************
    // TRACK RECORDING
    //*************************************************

    // Broadcast receivers do now show app chooser, so it's needed to give
    // them correct name of application package. For this reason, is required
    // LocusVersion object that specify which app will receive it's request

    /**
     * Main call to start track recording over API.
     *
     * @param ctx current context
     * @param lv  version of Locus used for track record
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionTrackRecordStart instead
    public static void actionTrackRecordStart(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        actionTrackRecordStart(ctx, lv, null);
    }

    /**
     * Main call to start track recording over API.
     *
     * @param ctx         current context
     * @param lv          version of Locus used for track record
     * @param profileName name of profile used for record (optional), otherwise last
     *                    used will be used for recording
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionTrackRecordStart instead
    public static void actionTrackRecordStart(Context ctx, LocusVersion lv, String profileName)
            throws RequiredVersionMissingException {
        // create basic intent
        Intent intent = actionTrackRecord(
                LocusConst.ACTION_TRACK_RECORD_START, lv);

        // set (optional) recording profile
        if (profileName != null && profileName.length() > 0) {
            intent.putExtra(LocusConst.INTENT_EXTRA_TRACK_REC_PROFILE, profileName);
        }

        // sent intent
        LocusUtils.sendBroadcast(ctx, intent, lv);
    }

    @Deprecated // use ActionBasics.actionTrackRecordPause instead
    public static void actionTrackRecordPause(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        // create intent
        Intent intent = actionTrackRecord(
                LocusConst.ACTION_TRACK_RECORD_PAUSE, lv);

        // sent intent
        LocusUtils.sendBroadcast(ctx, intent, lv);
    }

    @Deprecated // use ActionBasics.actionTrackRecordStop instead
    public static void actionTrackRecordStop(Context ctx, LocusVersion lv, boolean autoSave)
            throws RequiredVersionMissingException {
        // create intent
        Intent intent = actionTrackRecord(
                LocusConst.ACTION_TRACK_RECORD_STOP, lv);
        intent.putExtra(LocusConst.INTENT_EXTRA_TRACK_REC_AUTO_SAVE, autoSave);

        // sent intent
        LocusUtils.sendBroadcast(ctx, intent, lv);
    }

    // ADD WAYPOINT

    /**
     * Send broadcast to Locus to add a new waypoint to current track record.
     *
     * @param ctx current context
     * @param lv  version of Locus used for track record
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionTrackRecordAddWpt instead
    public static void actionTrackRecordAddWpt(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        actionTrackRecordAddWpt(ctx, lv, false);
    }

    /**
     * Send broadcast to Locus to add a new waypoint to current track record.
     *
     * @param ctx      current context
     * @param lv       version of Locus used for track record
     * @param autoSave <code>true</code> to automatically save waypoint without dialog
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionTrackRecordAddWpt instead
    public static void actionTrackRecordAddWpt(Context ctx, LocusVersion lv, boolean autoSave)
            throws RequiredVersionMissingException {
        actionTrackRecordAddWpt(ctx, lv, null, autoSave);
    }

    /**
     * Send broadcast to Locus to add a new waypoint to current track record.
     *
     * @param ctx      current context
     * @param lv       version of Locus used for track record
     * @param wptName  optional waypoint name
     * @param autoSave <code>true</code> to automatically save waypoint without dialog
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionTrackRecordAddWpt instead
    public static void actionTrackRecordAddWpt(Context ctx, LocusVersion lv,
            String wptName, boolean autoSave) throws RequiredVersionMissingException {
        Intent intent = actionTrackRecord(
                LocusConst.ACTION_TRACK_RECORD_ADD_WPT, lv);
        if (wptName != null && wptName.length() > 0) {
            intent.putExtra(LocusConst.INTENT_EXTRA_NAME, wptName);
        }
        intent.putExtra(LocusConst.INTENT_EXTRA_TRACK_REC_AUTO_SAVE, autoSave);

        // sent intent
        LocusUtils.sendBroadcast(ctx, intent, lv);
    }

    /**
     * Send broadcast to Locus to add a new waypoint to current track record.
     *
     * @param ctx         current context
     * @param lv          version of Locus used for track record
     * @param wptName     nameof waypoint
     * @param actionAfter action that may happen after (defined in LocusConst class)
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.actionTrackRecordAddWpt instead
    public static void actionTrackRecordAddWpt(Context ctx, LocusVersion lv,
            String wptName, String actionAfter) throws RequiredVersionMissingException {
        // generate basic intent
        Intent intent = actionTrackRecord(
                LocusConst.ACTION_TRACK_RECORD_ADD_WPT, lv);
        if (wptName != null && wptName.length() > 0) {
            intent.putExtra(LocusConst.INTENT_EXTRA_NAME, wptName);
        }

        // autosave is always disabled
        intent.putExtra(LocusConst.INTENT_EXTRA_TRACK_REC_AUTO_SAVE, false);

        // extra parameter
        intent.putExtra(LocusConst.INTENT_EXTRA_TRACK_REC_ACTION_AFTER, actionAfter);

        // sent intent
        LocusUtils.sendBroadcast(ctx, intent, lv);
    }

    /**
     * Private function that helps create basic intent that controls Locus.
     *
     * @param action action that should be performed
     * @param lv     version of Locus used for track record
     * @return created ready-to-use intent
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    private static Intent actionTrackRecord(String action, LocusVersion lv)
            throws RequiredVersionMissingException {
        // check version (available only in Free/Pro)
        int minVersion = VersionCode.UPDATE_02.vcFree;
        if (!LocusUtils.isLocusFreePro(lv, minVersion)) {
            throw new RequiredVersionMissingException(minVersion);
        }

        // generate and return intent
        Intent intent = new Intent(action);
        intent.setPackage(lv.getPackageName());
        return intent;
    }

    //*************************************************
    // TRACK RECORDING PROFILES
    //*************************************************

    /**
     * Simple container for track recording profiles.
     */
    @Deprecated // use ActionBasics.getTrackRecordingProfiles instead
    public static class TrackRecordProfileSimple extends Storable {

        private long mId;
        private String mName;
        private String mDesc;
        private byte[] mImg;

        /**
         * Empty constructor because of 'Storable'.
         */
        @SuppressWarnings("unused")
        public TrackRecordProfileSimple() {
            super();
            mId = 0L;
            mName = "";
            mDesc = "";
            mImg = null;
        }

        /**
         * Private constructor for track record profile.
         *
         * @param id   ID of profile
         * @param name name of profile
         * @param desc description of profile
         * @param img  image for profile
         */
        private TrackRecordProfileSimple(long id, String name, String desc, byte[] img) {
            this();
            this.mId = id;
            this.mName = name == null ? "" : name;
            this.mDesc = desc == null ? "" : desc;
            this.mImg = img;
        }

        /**
         * Get current profile ID.
         *
         * @return profile ID
         */
        public long getId() {
            return mId;
        }

        /**
         * Get current profile name.
         *
         * @return name of profile
         */
        public String getName() {
            return mName;
        }

        /**
         * Get profile generated description.
         *
         * @return profile description
         */
        public String getDesc() {
            return mDesc;
        }

        /**
         * Get current profile icon. Icon may be converted to bitmap object
         * thanks to 'Utils.getBitmap()' function.
         *
         * @return icon or 'null' if not defined or other problem happen
         */
        public byte[] getIcon() {
            return mImg;
        }

        // STORABLE PART

        @Override
        protected int getVersion() {
            return 0;
        }

        @Override
        protected void readObject(int version, DataReaderBigEndian dr) throws IOException {
            mId = dr.readLong();
            mName = dr.readString();
            mDesc = dr.readString();
            int imgSize = dr.readInt();
            if (imgSize > 0) {
                mImg = new byte[imgSize];
                dr.readBytes(mImg);
            }
        }

        @Override
        protected void writeObject(DataWriterBigEndian dw) throws IOException {
            dw.writeLong(mId);
            dw.writeString(mName);
            dw.writeString(mDesc);
            int imgSize = mImg != null ? mImg.length : 0;
            dw.writeInt(imgSize);
            if (imgSize > 0) {
                dw.write(mImg);
            }
        }
    }

    /**
     * Get list of available track recording profiles currently defined in app.
     *
     * @param ctx current context
     * @param lv  version of Locus that's asked
     * @return array of profiles, where first item in array is profile ID, second item is profile name
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.getTrackRecordingProfiles instead
    public static List<TrackRecordProfileSimple> getTrackRecordingProfiles(
            Context ctx, LocusVersion lv) throws RequiredVersionMissingException {
        // get scheme if valid Locus is available
        List<TrackRecordProfileSimple> profiles = new ArrayList<>();
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_09,
                LocusConst.CONTENT_PROVIDER_PATH_TRACK_RECORD_PROFILE_NAMES);

        // get data
        Cursor cursor = null;
        try {
            cursor = queryData(ctx, scheme, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return profiles;
            }

            // search in cursor for valid key
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                TrackRecordProfileSimple prof = new TrackRecordProfileSimple(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getBlob(3));
                profiles.add(prof);
            }
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getItemPurchaseState(" + ctx + ", " + lv + ")", e);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }

        // return 'unknown' state
        return profiles;
    }

    //*************************************************
    // WMS FUNCTIONS
    //*************************************************

    /*
      Add own WMS map
      ------------------------------------
      - this feature allow 3rd party application, add web address directly to list of WMS services in
      Map Manager screen / WMS tab
     */
    @Deprecated // use ActionBasics.callAddNewWmsMap instead
    public static void callAddNewWmsMap(Context context, String wmsUrl)
            throws RequiredVersionMissingException, InvalidObjectException {
        // check availability and start action
        if (!LocusUtils.isLocusAvailable(context, VersionCode.UPDATE_01)) {
            throw new RequiredVersionMissingException(VersionCode.UPDATE_01);
        }
        if (TextUtils.isEmpty(wmsUrl)) {
            throw new InvalidObjectException("WMS Url address \'" + wmsUrl + "\', is not valid!");
        }

        // call intent with WMS url
        Intent intent = new Intent(LocusConst.ACTION_ADD_NEW_WMS_MAP);
        intent.putExtra(LocusConst.INTENT_EXTRA_ADD_NEW_WMS_MAP_URL, wmsUrl);
        context.startActivity(intent);
    }

    //*************************************************
    // CONTENT OF LOCUS STORE
    //*************************************************

    /**
     * Allows to check if item with known ID is already purchased by user.
     *
     * @param ctx    current context
     * @param lv     version of Locus that's asked
     * @param itemId know ID of item
     * @return ItemPurchaseState state of purcahse
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.getItemPurchaseState instead
    public static int getItemPurchaseState(Context ctx, LocusVersion lv, long itemId)
            throws RequiredVersionMissingException {
        // get scheme if valid Locus is available
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_06,
                LocusConst.CONTENT_PROVIDER_PATH_ITEM_PURCHASE_STATE);
        scheme = ContentUris.withAppendedId(scheme, itemId);

        // get data
        Cursor cursor = null;
        try {
            cursor = queryData(ctx, scheme, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return LocusConst.PURCHASE_STATE_UNKNOWN;
            }

            // search for a valid key
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String key = cursor.getString(0);
                if (key.equals("purchaseState")) {
                    return cursor.getInt(1);
                }
            }
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getItemPurchaseState(" + ctx + ", " + lv + ")", e);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }

        // return 'unknown' state
        return LocusConst.PURCHASE_STATE_UNKNOWN;
    }

    /**
     * Start Locus and display certain item from Store defined by it's unique ID.
     *
     * @param ctx    current context
     * @param lv     known LocusVersion
     * @param itemId known item ID
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    @Deprecated // use ActionBasics.displayLocusStoreItemDetail instead
    public static void displayLocusStoreItemDetail(Context ctx, LocusVersion lv, long itemId)
            throws RequiredVersionMissingException {
        // check if application is available
        if (lv == null || !lv.isVersionValid(VersionCode.UPDATE_12)) {
            Logger.INSTANCE.logW(TAG, "displayLocusStoreItemDetail(), " +
                    "invalid Locus version");
            throw new RequiredVersionMissingException(VersionCode.UPDATE_12);
        }

        // call Locus
        Intent intent = new Intent(LocusConst.ACTION_DISPLAY_STORE_ITEM);
        intent.putExtra(LocusConst.INTENT_EXTRA_ITEM_ID, itemId);
        ctx.startActivity(intent);
    }

    // TODO
    // BELOW: PART TO REWRITE


    //*************************************************
    // MAP PREVIEW
    //*************************************************

    /**
     * Result container for screenshot request.
     */
    public static class BitmapLoadResult extends Storable {

        // loaded image
        private byte[] mImg;
        // number of not yet loaded tiles
        private int mNotYetLoadedTiles;

        /**
         * Empty constructor.
         */
        public BitmapLoadResult() {
            super();
            mImg = null;
            mNotYetLoadedTiles = 0;
        }

        /**
         * Private constructor.
         *
         * @param img               loaded image
         * @param notYetLoadedTiles number of tiles
         */
        private BitmapLoadResult(byte[] img, int notYetLoadedTiles) {
            super();
            this.mImg = img;
            this.mNotYetLoadedTiles = notYetLoadedTiles;
        }

        /**
         * Check if loaded result has valid image.
         *
         * @return <code>true</code> if image is valid
         */
        public boolean isValid() {
            return mImg != null;
        }

        /**
         * Get current loaded image tile.
         *
         * @return loaded image
         */
        public byte[] getImageB() {
            return mImg;
        }

        public Bitmap getImage() {
            return BitmapFactory.decodeByteArray(mImg, 0, mImg.length);
        }

        /**
         * Get number of missing (not yet loaded) tiles.
         *
         * @return number of tiles
         */
        public int getNumOfNotYetLoadedTiles() {
            return mNotYetLoadedTiles;
        }

        // STORABLE PART

        @Override
        protected int getVersion() {
            return 0;
        }

        @Override
        protected void readObject(int version, DataReaderBigEndian dr) {
            int size = dr.readInt();
            if (size > 0) {
                mImg = new byte[size];
                dr.readBytes(mImg);
                mNotYetLoadedTiles = dr.readInt();
            }
        }

        @Override
        protected void writeObject(DataWriterBigEndian dw) {
            if (mImg == null || mImg.length == 0) {
                dw.writeInt(0);
            } else {
                dw.writeInt(mImg.length);
                dw.write(mImg);
            }
            dw.writeInt(mNotYetLoadedTiles);
        }
    }

    /**
     * Get preview of current map screen.
     *
     * @param ctx       current context
     * @param lv        LocusVersion container
     * @param locCenter location of center
     * @param zoomValue zoom level
     * @param widthPx   required width in pixels
     * @param heightPx  required height in pixels
     * @return generated result
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static BitmapLoadResult getMapPreview(Context ctx, LocusVersion lv,
            Location locCenter, int zoomValue, int widthPx, int heightPx, boolean tinyMode)
            throws RequiredVersionMissingException {
        // get scheme if valid Locus is available
        Uri scheme = getProviderUriData(lv, VersionCode.UPDATE_04,
                LocusConst.CONTENT_PROVIDER_PATH_MAP_PREVIEW);

        // prepare base query with parameters
        String sbQuery =
                "lon=" + locCenter.getLongitude() + "," +
                        "lat=" + locCenter.getLatitude() + "," +
                        "zoom=" + zoomValue + "," +
                        "width=" + widthPx + "," +
                        "height=" + heightPx + "," +
                        "tinyMode=" + (tinyMode ? 1 : 0);

        // get data
        Cursor cursor = null;
        try {
            cursor = queryData(ctx, scheme, sbQuery);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            // load data
            byte[] img = null;
            int notYetLoadedTiles = 0;
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                String key = new String(cursor.getBlob(0));
                if (key.equals(LocusConst.VALUE_MAP_PREVIEW)) {
                    img = cursor.getBlob(1);
                } else if (key.equals(LocusConst.VALUE_MAP_PREVIEW_MISSING_TILES)) {
                    notYetLoadedTiles = cursor.getInt(1);
                }
            }

            // return result
            return new BitmapLoadResult(img, notYetLoadedTiles);
        } catch (Exception e) {
            Logger.INSTANCE.logE(TAG, "getMapPreview()", e);
            return new BitmapLoadResult(null, 0);
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }
    }

    //*************************************************
    // DYNAMIC PERIODIC UPDATES RECEIVER
    //*************************************************

    /**
     * Enable updates receiver for PeriodicUpdates. Use this function in case, you have defined received
     * in manifest file and you want to enable/disable received dynamically at runtime.
     *
     * @param ctx      current context
     * @param lv       Locus version to work with
     * @param receiver class that will be registered as receiver
     */
    public static void enablePeriodicUpdatesReceiver(Context ctx, LocusVersion lv,
            Class<? extends BroadcastReceiver> receiver) throws RequiredVersionMissingException {
        Logger.INSTANCE.logD(TAG, "enableReceiver(" + ctx + ")");
        PackageManager pm = ctx.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName(ctx, receiver),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        // notify about change
        refreshPeriodicUpdateListeners(ctx, lv);
    }

    /**
     * Disable updates receiver for PeriodicUpdates. Use this function in case, you have defined received
     * in manifest file and you want to enable/disable received dynamically at runtime.
     *
     * @param ctx      current context
     * @param lv       Locus version to work with
     * @param receiver class that will be registered as receiver
     */
    public static void disablePeriodicUpdatesReceiver(Context ctx, LocusVersion lv,
            Class<? extends BroadcastReceiver> receiver) throws RequiredVersionMissingException {
        Logger.INSTANCE.logD(TAG, "disableReceiver(" + ctx + ")");
        PackageManager pm = ctx.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName(ctx, receiver),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // notify about change
        refreshPeriodicUpdateListeners(ctx, lv);
    }

    /**
     * Send broadcast to refresh internal list of periodic update listeners. Use in case, app change active
     * receivers for periodic updates. Use this even in case, you unregister receiver, it may save some battery to user.
     *
     * @param ctx current context
     * @param lv  expected target locus
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static void refreshPeriodicUpdateListeners(Context ctx, LocusVersion lv)
            throws RequiredVersionMissingException {
        // check version (available only in Free/Pro)
        if (!LocusUtils.isLocusFreePro(lv, VersionCode.UPDATE_01.vcFree)) {
            throw new RequiredVersionMissingException(VersionCode.UPDATE_01);
        }

        // call intent
        Intent intent = new Intent(LocusConst.ACTION_REFRESH_PERIODIC_UPDATE_LISTENERS);

        // sent intent
        LocusUtils.sendBroadcast(ctx, intent, lv);
    }

    //*************************************************
    // WORK WITH CONTENT PROVIDERS
    //*************************************************

    /**
     * Query data from defined Uri.
     *
     * @param ctx current context
     * @param uri Uri to load data from
     * @return valid cursor with data or 'null' in case of empty or invalid cursor
     */
    public static Cursor queryData(Context ctx, Uri uri, String selection) {
        // generate cursor
        Cursor cursor = ctx.getContentResolver().query(uri,
                null, selection, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            Logger.INSTANCE.logE(TAG, "queryData(" + ctx + ", " + uri + "), " +
                    "invalid or empty cursor received");
            return null;
        }

        FrameLayout container = new FrameLayout(ctx) {
            public boolean onTouchEvent(MotionEvent event) {
                return super.onTouchEvent(event);
            }
        };

        return cursor;
    }

    /**
     * Query data from defined Uri and return loaded byte array content.
     *
     * @param ctx     current context
     * @param uri     Uri to load data from
     * @param keyName key under which we expect received data
     * @return valid cursor with data or 'null' in case of empty or invalid cursor
     */
    private static byte[] queryData(Context ctx, Uri uri, String selection, String keyName) {
        Cursor cursor = null;
        try {
            // get cursor data
            cursor = queryData(ctx, uri, selection);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            // handle query
            String key = cursor.getString(0);
            if (key.equals(keyName)) {
                return cursor.getBlob(1);
            }
        } finally {
            UtilsAnd.INSTANCE.closeQuietly(cursor);
        }

        // no data loaded
        Logger.INSTANCE.logW(TAG, "queryData(" + ctx + ", " + uri + ", " + keyName + "), " +
                "received data does not contains required key");
        return null;
    }

    /**
     * Get Uri to certain content in Locus Map data system.
     *
     * @param lv         Locus version we request to
     * @param requiredVc required minimal Locus version
     * @param path       path to data
     * @return generated Uri
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    private static Uri getProviderUriData(LocusVersion lv, VersionCode requiredVc, String path)
            throws RequiredVersionMissingException {
        return getProviderUri(lv, requiredVc,
                LocusConst.CONTENT_PROVIDER_AUTHORITY_DATA,
                path);
    }

    /**
     * Get Uri to certain content in Locus Map geocaching system.
     *
     * @param lv         Locus version we request to
     * @param requiredVc required minimal Locus version
     * @param path       path to data
     * @return generated Uri
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static Uri getProviderUrlGeocaching(LocusVersion lv, VersionCode requiredVc, String path)
            throws RequiredVersionMissingException {
        return getProviderUri(lv, requiredVc,
                LocusConst.CONTENT_PROVIDER_AUTHORITY_GEOCACHING,
                path);
    }

    /**
     * Get Uri to certain content in Locus Map app.
     *
     * @param lv         Locus version we request to
     * @param requiredVc required minimal Locus version
     * @param provider   provider for data
     * @param path       path to data
     * @return generated Uri
     * @throws RequiredVersionMissingException if Locus in required version is missing
     */
    public static Uri getProviderUri(LocusVersion lv, VersionCode requiredVc,
            String provider, String path) throws RequiredVersionMissingException {
        // check URI parts ( should not happen, just check )
        if (provider == null || provider.length() == 0 ||
                path == null || path.length() == 0) {
            Logger.INSTANCE.logW(TAG, "getProviderUri(), " +
                    "invalid 'authority' or 'path'parameters");
            throw new RequiredVersionMissingException(requiredVc);
        }

        // check if application is available
        if (lv == null || requiredVc == null || !lv.isVersionValid(requiredVc)) {
            Logger.INSTANCE.logW(TAG, "getProviderUri(), " +
                    "invalid Locus version");
            throw new RequiredVersionMissingException(requiredVc);
        }

        // generate content provider by type
        StringBuilder sb = new StringBuilder();
        if (lv.isVersionFree()) {
            sb.append("content://menion.android.locus.free");
        } else if (lv.isVersionPro()) {
            sb.append("content://menion.android.locus.pro");
        } else if (lv.isVersionGis()) {
            sb.append("content://menion.android.locus.gis");
        } else {
            Logger.INSTANCE.logW(TAG, "getProviderUri(), " +
                    "unknown Locus version:" + lv);
            throw new RequiredVersionMissingException(requiredVc);
        }

        // finish URI
        return Uri.parse(sb.append(".").append(provider).
                append("/").append(path).toString());
    }
}
