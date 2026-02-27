package com.pulse.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LectureDao_Impl implements LectureDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Lecture> __insertionAdapterOfLecture;

  private final EntityDeletionOrUpdateAdapter<Lecture> __updateAdapterOfLecture;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public LectureDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLecture = new EntityInsertionAdapter<Lecture>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `lectures` (`id`,`name`,`videoId`,`pdfId`,`pdfLocalPath`,`videoLocalPath`,`isPdfDownloaded`,`isLocal`,`lastPosition`,`videoDuration`,`lastPage`,`speed`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Lecture entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getVideoId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getVideoId());
        }
        if (entity.getPdfId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPdfId());
        }
        statement.bindString(5, entity.getPdfLocalPath());
        if (entity.getVideoLocalPath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getVideoLocalPath());
        }
        final int _tmp = entity.isPdfDownloaded() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final int _tmp_1 = entity.isLocal() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getLastPosition());
        statement.bindLong(10, entity.getVideoDuration());
        statement.bindLong(11, entity.getLastPage());
        statement.bindDouble(12, entity.getSpeed());
        statement.bindLong(13, entity.getUpdatedAt());
      }
    };
    this.__updateAdapterOfLecture = new EntityDeletionOrUpdateAdapter<Lecture>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `lectures` SET `id` = ?,`name` = ?,`videoId` = ?,`pdfId` = ?,`pdfLocalPath` = ?,`videoLocalPath` = ?,`isPdfDownloaded` = ?,`isLocal` = ?,`lastPosition` = ?,`videoDuration` = ?,`lastPage` = ?,`speed` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Lecture entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getVideoId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getVideoId());
        }
        if (entity.getPdfId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getPdfId());
        }
        statement.bindString(5, entity.getPdfLocalPath());
        if (entity.getVideoLocalPath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getVideoLocalPath());
        }
        final int _tmp = entity.isPdfDownloaded() ? 1 : 0;
        statement.bindLong(7, _tmp);
        final int _tmp_1 = entity.isLocal() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getLastPosition());
        statement.bindLong(10, entity.getVideoDuration());
        statement.bindLong(11, entity.getLastPage());
        statement.bindDouble(12, entity.getSpeed());
        statement.bindLong(13, entity.getUpdatedAt());
        statement.bindString(14, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM lectures WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Lecture lecture, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLecture.insert(lecture);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<Lecture> list, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLecture.insert(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Lecture lecture, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLecture.handle(lecture);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Lecture>> getDriveLectures() {
    final String _sql = "SELECT * FROM lectures WHERE isLocal = 0 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lectures"}, new Callable<List<Lecture>>() {
      @Override
      @NonNull
      public List<Lecture> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfPdfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfLocalPath");
          final int _cursorIndexOfVideoLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "videoLocalPath");
          final int _cursorIndexOfIsPdfDownloaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isPdfDownloaded");
          final int _cursorIndexOfIsLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocal");
          final int _cursorIndexOfLastPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPosition");
          final int _cursorIndexOfVideoDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "videoDuration");
          final int _cursorIndexOfLastPage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPage");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<Lecture> _result = new ArrayList<Lecture>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Lecture _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpVideoId;
            if (_cursor.isNull(_cursorIndexOfVideoId)) {
              _tmpVideoId = null;
            } else {
              _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            }
            final String _tmpPdfId;
            if (_cursor.isNull(_cursorIndexOfPdfId)) {
              _tmpPdfId = null;
            } else {
              _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            }
            final String _tmpPdfLocalPath;
            _tmpPdfLocalPath = _cursor.getString(_cursorIndexOfPdfLocalPath);
            final String _tmpVideoLocalPath;
            if (_cursor.isNull(_cursorIndexOfVideoLocalPath)) {
              _tmpVideoLocalPath = null;
            } else {
              _tmpVideoLocalPath = _cursor.getString(_cursorIndexOfVideoLocalPath);
            }
            final boolean _tmpIsPdfDownloaded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPdfDownloaded);
            _tmpIsPdfDownloaded = _tmp != 0;
            final boolean _tmpIsLocal;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLocal);
            _tmpIsLocal = _tmp_1 != 0;
            final long _tmpLastPosition;
            _tmpLastPosition = _cursor.getLong(_cursorIndexOfLastPosition);
            final long _tmpVideoDuration;
            _tmpVideoDuration = _cursor.getLong(_cursorIndexOfVideoDuration);
            final int _tmpLastPage;
            _tmpLastPage = _cursor.getInt(_cursorIndexOfLastPage);
            final float _tmpSpeed;
            _tmpSpeed = _cursor.getFloat(_cursorIndexOfSpeed);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new Lecture(_tmpId,_tmpName,_tmpVideoId,_tmpPdfId,_tmpPdfLocalPath,_tmpVideoLocalPath,_tmpIsPdfDownloaded,_tmpIsLocal,_tmpLastPosition,_tmpVideoDuration,_tmpLastPage,_tmpSpeed,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Lecture>> getLocalLectures() {
    final String _sql = "SELECT * FROM lectures WHERE isLocal = 1 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lectures"}, new Callable<List<Lecture>>() {
      @Override
      @NonNull
      public List<Lecture> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfPdfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfLocalPath");
          final int _cursorIndexOfVideoLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "videoLocalPath");
          final int _cursorIndexOfIsPdfDownloaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isPdfDownloaded");
          final int _cursorIndexOfIsLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocal");
          final int _cursorIndexOfLastPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPosition");
          final int _cursorIndexOfVideoDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "videoDuration");
          final int _cursorIndexOfLastPage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPage");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<Lecture> _result = new ArrayList<Lecture>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Lecture _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpVideoId;
            if (_cursor.isNull(_cursorIndexOfVideoId)) {
              _tmpVideoId = null;
            } else {
              _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            }
            final String _tmpPdfId;
            if (_cursor.isNull(_cursorIndexOfPdfId)) {
              _tmpPdfId = null;
            } else {
              _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            }
            final String _tmpPdfLocalPath;
            _tmpPdfLocalPath = _cursor.getString(_cursorIndexOfPdfLocalPath);
            final String _tmpVideoLocalPath;
            if (_cursor.isNull(_cursorIndexOfVideoLocalPath)) {
              _tmpVideoLocalPath = null;
            } else {
              _tmpVideoLocalPath = _cursor.getString(_cursorIndexOfVideoLocalPath);
            }
            final boolean _tmpIsPdfDownloaded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPdfDownloaded);
            _tmpIsPdfDownloaded = _tmp != 0;
            final boolean _tmpIsLocal;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLocal);
            _tmpIsLocal = _tmp_1 != 0;
            final long _tmpLastPosition;
            _tmpLastPosition = _cursor.getLong(_cursorIndexOfLastPosition);
            final long _tmpVideoDuration;
            _tmpVideoDuration = _cursor.getLong(_cursorIndexOfVideoDuration);
            final int _tmpLastPage;
            _tmpLastPage = _cursor.getInt(_cursorIndexOfLastPage);
            final float _tmpSpeed;
            _tmpSpeed = _cursor.getFloat(_cursorIndexOfSpeed);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new Lecture(_tmpId,_tmpName,_tmpVideoId,_tmpPdfId,_tmpPdfLocalPath,_tmpVideoLocalPath,_tmpIsPdfDownloaded,_tmpIsLocal,_tmpLastPosition,_tmpVideoDuration,_tmpLastPage,_tmpSpeed,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Lecture> getById(final String id) {
    final String _sql = "SELECT * FROM lectures WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lectures"}, new Callable<Lecture>() {
      @Override
      @Nullable
      public Lecture call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfVideoId = CursorUtil.getColumnIndexOrThrow(_cursor, "videoId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfPdfLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfLocalPath");
          final int _cursorIndexOfVideoLocalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "videoLocalPath");
          final int _cursorIndexOfIsPdfDownloaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isPdfDownloaded");
          final int _cursorIndexOfIsLocal = CursorUtil.getColumnIndexOrThrow(_cursor, "isLocal");
          final int _cursorIndexOfLastPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPosition");
          final int _cursorIndexOfVideoDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "videoDuration");
          final int _cursorIndexOfLastPage = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPage");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final Lecture _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpVideoId;
            if (_cursor.isNull(_cursorIndexOfVideoId)) {
              _tmpVideoId = null;
            } else {
              _tmpVideoId = _cursor.getString(_cursorIndexOfVideoId);
            }
            final String _tmpPdfId;
            if (_cursor.isNull(_cursorIndexOfPdfId)) {
              _tmpPdfId = null;
            } else {
              _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            }
            final String _tmpPdfLocalPath;
            _tmpPdfLocalPath = _cursor.getString(_cursorIndexOfPdfLocalPath);
            final String _tmpVideoLocalPath;
            if (_cursor.isNull(_cursorIndexOfVideoLocalPath)) {
              _tmpVideoLocalPath = null;
            } else {
              _tmpVideoLocalPath = _cursor.getString(_cursorIndexOfVideoLocalPath);
            }
            final boolean _tmpIsPdfDownloaded;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPdfDownloaded);
            _tmpIsPdfDownloaded = _tmp != 0;
            final boolean _tmpIsLocal;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsLocal);
            _tmpIsLocal = _tmp_1 != 0;
            final long _tmpLastPosition;
            _tmpLastPosition = _cursor.getLong(_cursorIndexOfLastPosition);
            final long _tmpVideoDuration;
            _tmpVideoDuration = _cursor.getLong(_cursorIndexOfVideoDuration);
            final int _tmpLastPage;
            _tmpLastPage = _cursor.getInt(_cursorIndexOfLastPage);
            final float _tmpSpeed;
            _tmpSpeed = _cursor.getFloat(_cursorIndexOfSpeed);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new Lecture(_tmpId,_tmpName,_tmpVideoId,_tmpPdfId,_tmpPdfLocalPath,_tmpVideoLocalPath,_tmpIsPdfDownloaded,_tmpIsLocal,_tmpLastPosition,_tmpVideoDuration,_tmpLastPage,_tmpSpeed,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object deleteMissingDrive(final List<String> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM lectures WHERE isLocal = 0 AND id NOT IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (String _item : ids) {
          _stmt.bindString(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
