package blue.fen.startup;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * <p>创建时间：2024/02/18 （星期日｝</p>
 * <p>作者： blue_fen</p>
 * <p>描述：启动任务</p>
 */
public class BFStartupProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        try {
            BFProjectParser.parserMetaData(getContext()).execute();
        } catch (Throwable e) {
            throw new BFStartupException(e);
        }
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
