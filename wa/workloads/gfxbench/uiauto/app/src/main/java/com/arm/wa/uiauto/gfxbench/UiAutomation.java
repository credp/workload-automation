/*    Copyright 2014-2016 ARM Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.wa.uiauto.gfxbench;

import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.support.test.uiautomator.UiScrollable;
import android.util.Log;
import android.graphics.Rect;

import com.arm.wa.uiauto.BaseUiAutomation;
import com.arm.wa.uiauto.ActionLogger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class UiAutomation extends BaseUiAutomation {

    private int networkTimeoutSecs = 30;
    private long networkTimeout =  TimeUnit.SECONDS.toMillis(networkTimeoutSecs);
    public static String TAG = "UXPERF";
    protected Bundle parameters;
    protected String[] testList;

    @Before
    public void initialize(){
        parameters = getParams();
        testList = parameters.getStringArray("tests");
    }

    @Test
    public void setup() throws Exception{
        setScreenOrientation(ScreenOrientation.NATURAL);
        clearFirstRun();

        //Calculate the location of the test selection button
        UiObject circle =
            mDevice.findObject(new UiSelector().resourceId("net.kishonti.gfxbench.gl.v50000.corporate:id/main_circleControl")
            .className("android.widget.RelativeLayout"));
        Rect bounds = circle.getBounds();
        int selectx = bounds.width()/4;
        selectx = bounds.centerX() + selectx;
        int selecty = bounds.height()/4;
        selecty = bounds.centerY() + selecty;

        Log.d(TAG, "maxx " + selectx);
        Log.d(TAG, "maxy " + selecty);

        mDevice.click(selectx,selecty);

        // Disable test categories
        toggleTest("High-Level Tests");
        toggleTest("Low-Level Tests");
        toggleTest("Special Tests");
        toggleTest("Fixed Time Test");

        // Enable selected tests
        for (String test : testList) {
            toggleTest(test);
        }
    }


    @Test
    public void runWorkload() throws Exception {
        runBenchmark();
        getScores();
    }

    @Test
    public void teardown() throws Exception{
        unsetScreenOrientation();
    }

    public void clearFirstRun() throws Exception {
        UiObject accept =
            mDevice.findObject(new UiSelector().resourceId("android:id/button1")
                .className("android.widget.Button"));
        if (accept.exists()){
            accept.click();
            sleep(5);
        }
        UiObject sync =
                mDevice.findObject(new UiSelector().text("Data synchronization")
                    .className("android.widget.TextView"));
        if (!sync.exists()){
            sync = mDevice.findObject(new UiSelector().text("Pushed data not found")
                    .className("android.widget.TextView"));
        }
        if (sync.exists()){
            UiObject data =
                mDevice.findObject(new UiSelector().resourceId("android:id/button1")
                    .className("android.widget.Button"));
            data.click();
        }

        UiObject home =
            mDevice.findObject(new UiSelector().resourceId("net.kishonti.gfxbench.gl.v50000.corporate:id/main_homeBack")
                .className("android.widget.LinearLayout"));
            home.waitForExists(300000);
    }

    public void runBenchmark() throws Exception {
        //Start the tests
        UiObject start =
            mDevice.findObject(new UiSelector().text("Start"));
        start.click();

        //Wait for results
        UiObject complete =
            mDevice.findObject(new UiSelector().textContains("Test")
                .className("android.widget.TextView"));
        complete.waitForExists(1200000);

        UiObject outOfmemory = mDevice.findObject(new UiSelector().text("OUT_OF_MEMORY"));
        if (outOfmemory.exists()) {
            throw new OutOfMemoryError("The workload has failed because the device is doing to much work.");
        }
    }

    public void getScores() throws Exception {
        UiScrollable list = new UiScrollable(new UiSelector().scrollable(true));

        UiObject results =
            mDevice.findObject(new UiSelector().resourceId("net.kishonti.gfxbench.gl.v50000.corporate:id/results_testList"));

        for (String test : testList) {
            getTestScore(list, results, test);
        }

    }

    public void toggleTest(String testname) throws Exception {
        UiScrollable list = new UiScrollable(new UiSelector().scrollable(true));
        UiObject test =
            mDevice.findObject(new UiSelector().text(testname));
        if (!test.exists() && list.waitForExists(60)) {
            list.flingToBeginning(10);
            list.scrollIntoView(test);
        }
        test.click();
    }

    public void getTestScore(UiScrollable scrollable, UiObject resultsList, String test) throws Exception {
        if (test.equals("Tessellation")) {
            scrollable.scrollToEnd(1000);
        }
        for (int i=1; i < resultsList.getChildCount(); i++) {
            UiObject testname = resultsList.getChild(new UiSelector().index(i))
                .getChild(new UiSelector().resourceId("net.kishonti.gfxbench.gl.v50000.corporate:id/updated_result_item_name"));
            if (testname.exists() && testname.getText().equals(test)) {
                UiObject result = resultsList.getChild(new UiSelector()
                                    .index(i))
                                    .getChild(new UiSelector()
                                    .resourceId("net.kishonti.gfxbench.gl.v50000.corporate:id/updated_result_item_subresult"));
                Log.d(TAG, test + " score " + result.getText());
                return;
            }
        }
    }
}
