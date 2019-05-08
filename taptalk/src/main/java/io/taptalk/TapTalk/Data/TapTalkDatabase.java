package io.taptalk.TapTalk.Data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import io.taptalk.TapTalk.Data.Contact.TAPMyContactDao;
import io.taptalk.TapTalk.Data.Message.TAPMessageDao;
import io.taptalk.TapTalk.Data.Message.TAPMessageEntity;
import io.taptalk.TapTalk.Data.RecentSearch.TAPRecentSearchDao;
import io.taptalk.TapTalk.Data.RecentSearch.TAPRecentSearchEntity;
import io.taptalk.TapTalk.Model.TAPUserModel;

import static io.taptalk.TapTalk.Const.TAPDefaultConstant.RoomDatabase.kDatabaseVersion;

@Database(entities = {TAPMessageEntity.class, TAPRecentSearchEntity.class, TAPUserModel.class}, version = kDatabaseVersion, exportSchema = false)
public abstract class TapTalkDatabase extends RoomDatabase{

    private static TapTalkDatabase database;

    // TODO: 16/10/18 kalau udah di deploy jangan lupa di encrypt
    public static TapTalkDatabase getDatabase(Context context){
        if (null == database){
//            SafeHelperFactory factory = SafeHelperFactory.fromUser(
//                    Editable.Factory.getInstance().newEditable(DB_ENCRYPT_PASS));
            database = Room.databaseBuilder(context,
                    TapTalkDatabase.class, "message_database")
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
//                    .openHelperFactory(factory)
                    .build();
        }

        return database;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX index_MyContact_isContact ON MyContact(isContact)");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE 'MyContact' ADD COLUMN 'phoneWithCode' TEXT");
            database.execSQL("ALTER TABLE 'MyContact' ADD COLUMN 'isEmailVerified' INTEGER");
            database.execSQL("ALTER TABLE 'MyContact' ADD COLUMN 'isPhoneVerified' INTEGER");
            database.execSQL("ALTER TABLE 'MyContact' ADD COLUMN 'countryID' INTEGER");
            database.execSQL("ALTER TABLE 'MyContact' ADD COLUMN 'countryCallingCode' TEXT");
        }
    };

    public abstract TAPMessageDao messageDao();
    public abstract TAPRecentSearchDao recentSearchDao();
    public abstract TAPMyContactDao myContactDao();
}
