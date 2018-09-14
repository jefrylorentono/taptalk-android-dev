package com.moselo.HomingPigeon.ViewModel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.moselo.HomingPigeon.Model.UserModel;

import java.util.ArrayList;
import java.util.List;

public class ContactListViewModel extends AndroidViewModel {

    // Contact List needs to be sorted ascending by name
    private List<UserModel> contactList;
    private List<UserModel> filteredContacts;

    public ContactListViewModel(@NonNull Application application) {
        super(application);
        contactList = new ArrayList<>();
        filteredContacts = new ArrayList<>();
    }

    public List<UserModel> getContactList() {
        return contactList;
    }

    public void setContactList(List<UserModel> contactList) {
        this.contactList = contactList;
    }

    public List<UserModel> getFilteredContacts() {
        return filteredContacts;
    }

    public void setFilteredContacts(List<UserModel> filteredContacts) {
        this.filteredContacts = filteredContacts;
    }
}
