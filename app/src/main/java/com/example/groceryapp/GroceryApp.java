package com.example.groceryapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.groceryapp.utils.Utils;
import com.example.groceryapp.viewModels.UserViewModel;

public class GroceryApp extends Application implements ViewModelStoreOwner {

    private ViewModelStore viewModelStore;
    private UserViewModel userViewModel;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Utils (if needed)
        Utils.init(this);

        // Create ViewModelStore for global ViewModels
        viewModelStore = new ViewModelStore();

        // Create UserViewModel with factory and assign it to global instance
        UserViewModelFactory factory = new UserViewModelFactory(this);
        userViewModel = new ViewModelProvider(this, factory).get(UserViewModel.class);
    }

    public UserViewModel getUserViewModel() {
        return userViewModel;
    }

    @NonNull
    @Override
    public ViewModelStore getViewModelStore() {
        return viewModelStore;
    }
}
