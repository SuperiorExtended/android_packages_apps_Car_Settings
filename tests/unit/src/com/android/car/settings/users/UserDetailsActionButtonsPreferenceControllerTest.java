/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.car.settings.users;

import static android.content.pm.UserInfo.FLAG_ADMIN;
import static android.content.pm.UserInfo.FLAG_INITIALIZED;

import static com.android.car.settings.common.ActionButtonsPreference.ActionButtons;
import static com.android.car.settings.users.UserDetailsActionButtonsPreferenceController.MAKE_ADMIN_DIALOG_TAG;
import static com.android.car.settings.users.UserDetailsActionButtonsPreferenceController.REMOVE_USER_DIALOG_TAG;
import static com.android.car.settings.users.UsersDialogProvider.ANY_USER;
import static com.android.car.settings.users.UsersDialogProvider.KEY_USER_TYPE;
import static com.android.car.settings.users.UsersDialogProvider.LAST_ADMIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.settings.common.ActionButtonInfo;
import com.android.car.settings.common.ActionButtonsPreference;
import com.android.car.settings.common.ConfirmationDialogFragment;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceControllerTestUtil;
import com.android.car.settings.testutils.TestLifecycleOwner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class UserDetailsActionButtonsPreferenceControllerTest {
    private static final String TEST_USERNAME = "Test Username";

    private Context mContext = ApplicationProvider.getApplicationContext();
    private LifecycleOwner mLifecycleOwner;
    private ActionButtonsPreference mPreference;
    private CarUxRestrictions mCarUxRestrictions;
    private UserDetailsActionButtonsPreferenceController mPreferenceController;

    @Mock
    private FragmentController mFragmentController;
    @Mock
    private UserHelper mMockUserHelper;
    @Mock
    private UserManager mMockUserManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = new TestLifecycleOwner();

        mCarUxRestrictions = new CarUxRestrictions.Builder(/* reqOpt= */ true,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, /* timestamp= */ 0).build();

        mPreference = new ActionButtonsPreference(mContext);
        mPreferenceController = new UserDetailsActionButtonsPreferenceController(mContext,
                /* preferenceKey= */ "key", mFragmentController, mCarUxRestrictions);
        mPreferenceController.setUserHelper(mMockUserHelper);
        mPreferenceController.setUserManager(mMockUserManager);
    }

    @Test
    public void onCreate_isCurrentUser_renameButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getRenameButton().isVisible()).isTrue();
    }

    @Test
    public void onCreate_isNotCurrentUser_renameButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getRenameButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_isAdminViewingNonAdmin_makeAdminButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getMakeAdminButton().isVisible()).isTrue();
    }

    @Test
    public void onCreate_isAdminViewingAdmin_makeAdminButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME,
                FLAG_INITIALIZED | FLAG_ADMIN);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getMakeAdminButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_userNotRestricted_deleteButtonShown() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getDeleteButton().isVisible()).isTrue();
    }

    @Test
    public void onCreate_userRestricted_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.hasUserRestriction(UserManager.DISALLOW_REMOVE_USER))
                .thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getDeleteButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_viewingSystemUser_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 0, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getDeleteButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_isDemoUser_deleteButtonHidden() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isDemoUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(getDeleteButton().isVisible()).isFalse();
    }

    @Test
    public void onCreate_hasPreviousMakeAdminDialog_dialogListenerSet() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(
                mContext).build();
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mFragmentController.findDialogByTag(
                MAKE_ADMIN_DIALOG_TAG)).thenReturn(dialog);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(dialog.getConfirmListener()).isNotNull();
    }

    @Test
    public void onCreate_hasPreviousDeleteDialog_dialogListenerSet() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(
                mContext).build();
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        when(mFragmentController.findDialogByTag(
                REMOVE_USER_DIALOG_TAG)).thenReturn(dialog);
        mPreferenceController.onCreate(mLifecycleOwner);

        assertThat(dialog.getConfirmListener()).isNotNull();
    }

    @Test
    public void onRenameButtonClicked_launchEditUsernameFragment() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        getRenameButton().getOnClickListener().onClick(/* view= */ null);

        verify(mFragmentController).launchFragment(any(EditUsernameFragment.class));
    }

    @Test
    public void onMakeAdminButtonClicked_showsConfirmationDialog() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        getMakeAdminButton().getOnClickListener().onClick(/* view= */ null);

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(MAKE_ADMIN_DIALOG_TAG));
    }

    @Test
    public void onDeleteButtonClicked_showsConfirmationDialog() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        getDeleteButton().getOnClickListener().onClick(/* view= */ null);

        verify(mFragmentController).showDialog(any(ConfirmationDialogFragment.class),
                eq(REMOVE_USER_DIALOG_TAG));
    }

    @Test
    @Ignore("b/173179832, b/172513940")
    public void onMakeAdminConfirmed_makeUserAdmin() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);

        mPreferenceController.onCreate(mLifecycleOwner);
        Bundle arguments = new Bundle();
        arguments.putParcelable(UsersDialogProvider.KEY_USER_TO_MAKE_ADMIN, userInfo);
        mPreferenceController.mMakeAdminConfirmListener.onConfirm(arguments);

        // verify android.car.userlib.UserHelper.grantAdminPermissions called
    }

    @Test
    public void onMakeAdminConfirmed_goBack() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        when(mMockUserManager.isAdminUser()).thenReturn(true);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        Bundle arguments = new Bundle();
        arguments.putParcelable(UsersDialogProvider.KEY_USER_TO_MAKE_ADMIN, userInfo);
        mPreferenceController.mMakeAdminConfirmListener.onConfirm(arguments);

        verify(mFragmentController).goBack();
    }

    @Test
    public void onDeleteConfirmed_removeUser() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        Bundle arguments = new Bundle();
        arguments.putString(KEY_USER_TYPE, ANY_USER);
        mPreferenceController.mRemoveConfirmListener.onConfirm(arguments);

        verify(mMockUserHelper).removeUser(mContext, userInfo);
    }

    @Test
    @UiThreadTest
    public void onDeleteConfirmed_lastAdmin_launchChooseNewAdminFragment() {
        UserInfo userInfo = new UserInfo(/* id= */ 10, TEST_USERNAME, FLAG_INITIALIZED);
        when(mMockUserHelper.isCurrentProcessUser(userInfo)).thenReturn(false);
        mPreferenceController.setUserInfo(userInfo);
        PreferenceControllerTestUtil.assignPreference(mPreferenceController, mPreference);
        mPreferenceController.onCreate(mLifecycleOwner);

        Bundle arguments = new Bundle();
        arguments.putString(KEY_USER_TYPE, LAST_ADMIN);
        mPreferenceController.mRemoveConfirmListener.onConfirm(arguments);

        verify(mFragmentController).launchFragment(any(ChooseNewAdminFragment.class));
    }

    private ActionButtonInfo getRenameButton() {
        return mPreference.getButton(ActionButtons.BUTTON1);
    }

    private ActionButtonInfo getMakeAdminButton() {
        return mPreference.getButton(ActionButtons.BUTTON2);
    }

    private ActionButtonInfo getDeleteButton() {
        return mPreference.getButton(ActionButtons.BUTTON3);
    }
}