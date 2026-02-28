package com.pulse.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pulse.core.data.db.NoteVisual;
import com.pulse.core.data.db.VisualType;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class NoteVisualDao_Impl implements NoteVisualDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NoteVisual> __insertionAdapterOfNoteVisual;

  private final EntityDeletionOrUpdateAdapter<NoteVisual> __updateAdapterOfNoteVisual;

  private final SharedSQLiteStatement __preparedStmtOfMarkDeleted;

  public NoteVisualDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNoteVisual = new EntityInsertionAdapter<NoteVisual>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `note_visuals` (`id`,`lectureId`,`pdfId`,`timestamp`,`pageNumber`,`type`,`data`,`color`,`strokeWidth`,`alpha`,`hlcTimestamp`,`isDeleted`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteVisual entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getLectureId());
        statement.bindString(3, entity.getPdfId());
        statement.bindLong(4, entity.getTimestamp());
        statement.bindLong(5, entity.getPageNumber());
        statement.bindString(6, __VisualType_enumToString(entity.getType()));
        statement.bindString(7, entity.getData());
        statement.bindLong(8, entity.getColor());
        statement.bindDouble(9, entity.getStrokeWidth());
        statement.bindDouble(10, entity.getAlpha());
        statement.bindString(11, entity.getHlcTimestamp());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(12, _tmp);
        statement.bindLong(13, entity.getUpdatedAt());
      }
    };
    this.__updateAdapterOfNoteVisual = new EntityDeletionOrUpdateAdapter<NoteVisual>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `note_visuals` SET `id` = ?,`lectureId` = ?,`pdfId` = ?,`timestamp` = ?,`pageNumber` = ?,`type` = ?,`data` = ?,`color` = ?,`strokeWidth` = ?,`alpha` = ?,`hlcTimestamp` = ?,`isDeleted` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteVisual entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getLectureId());
        statement.bindString(3, entity.getPdfId());
        statement.bindLong(4, entity.getTimestamp());
        statement.bindLong(5, entity.getPageNumber());
        statement.bindString(6, __VisualType_enumToString(entity.getType()));
        statement.bindString(7, entity.getData());
        statement.bindLong(8, entity.getColor());
        statement.bindDouble(9, entity.getStrokeWidth());
        statement.bindDouble(10, entity.getAlpha());
        statement.bindString(11, entity.getHlcTimestamp());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(12, _tmp);
        statement.bindLong(13, entity.getUpdatedAt());
        statement.bindLong(14, entity.getId());
      }
    };
    this.__preparedStmtOfMarkDeleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE note_visuals SET isDeleted = 1, hlcTimestamp = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final NoteVisual visual, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNoteVisual.insert(visual);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final NoteVisual visual, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfNoteVisual.handle(visual);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertWithCrdt(final NoteVisual visual,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> NoteVisualDao.DefaultImpls.insertWithCrdt(NoteVisualDao_Impl.this, visual, __cont), $completion);
  }

  @Override
  public Object markDeleted(final long id, final String hlcTimestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkDeleted.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, hlcTimestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfMarkDeleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NoteVisual>> getVisualsForFile(final String lectureId, final String pdfId) {
    final String _sql = "SELECT * FROM note_visuals WHERE lectureId = ? AND pdfId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, lectureId);
    _argIndex = 2;
    _statement.bindString(_argIndex, pdfId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"note_visuals"}, new Callable<List<NoteVisual>>() {
      @Override
      @NonNull
      public List<NoteVisual> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLectureId = CursorUtil.getColumnIndexOrThrow(_cursor, "lectureId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfData = CursorUtil.getColumnIndexOrThrow(_cursor, "data");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfStrokeWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeWidth");
          final int _cursorIndexOfAlpha = CursorUtil.getColumnIndexOrThrow(_cursor, "alpha");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<NoteVisual> _result = new ArrayList<NoteVisual>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteVisual _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLectureId;
            _tmpLectureId = _cursor.getString(_cursorIndexOfLectureId);
            final String _tmpPdfId;
            _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final VisualType _tmpType;
            _tmpType = __VisualType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final String _tmpData;
            _tmpData = _cursor.getString(_cursorIndexOfData);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final float _tmpStrokeWidth;
            _tmpStrokeWidth = _cursor.getFloat(_cursorIndexOfStrokeWidth);
            final float _tmpAlpha;
            _tmpAlpha = _cursor.getFloat(_cursorIndexOfAlpha);
            final String _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getString(_cursorIndexOfHlcTimestamp);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new NoteVisual(_tmpId,_tmpLectureId,_tmpPdfId,_tmpTimestamp,_tmpPageNumber,_tmpType,_tmpData,_tmpColor,_tmpStrokeWidth,_tmpAlpha,_tmpHlcTimestamp,_tmpIsDeleted,_tmpUpdatedAt);
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
  public Object getUpdatesSince(final String since,
      final Continuation<? super List<NoteVisual>> $completion) {
    final String _sql = "SELECT * FROM note_visuals WHERE hlcTimestamp > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<NoteVisual>>() {
      @Override
      @NonNull
      public List<NoteVisual> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLectureId = CursorUtil.getColumnIndexOrThrow(_cursor, "lectureId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfData = CursorUtil.getColumnIndexOrThrow(_cursor, "data");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfStrokeWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeWidth");
          final int _cursorIndexOfAlpha = CursorUtil.getColumnIndexOrThrow(_cursor, "alpha");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<NoteVisual> _result = new ArrayList<NoteVisual>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteVisual _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLectureId;
            _tmpLectureId = _cursor.getString(_cursorIndexOfLectureId);
            final String _tmpPdfId;
            _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final VisualType _tmpType;
            _tmpType = __VisualType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final String _tmpData;
            _tmpData = _cursor.getString(_cursorIndexOfData);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final float _tmpStrokeWidth;
            _tmpStrokeWidth = _cursor.getFloat(_cursorIndexOfStrokeWidth);
            final float _tmpAlpha;
            _tmpAlpha = _cursor.getFloat(_cursorIndexOfAlpha);
            final String _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getString(_cursorIndexOfHlcTimestamp);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new NoteVisual(_tmpId,_tmpLectureId,_tmpPdfId,_tmpTimestamp,_tmpPageNumber,_tmpType,_tmpData,_tmpColor,_tmpStrokeWidth,_tmpAlpha,_tmpHlcTimestamp,_tmpIsDeleted,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByIdSync(final long id, final Continuation<? super NoteVisual> $completion) {
    final String _sql = "SELECT * FROM note_visuals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<NoteVisual>() {
      @Override
      @Nullable
      public NoteVisual call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLectureId = CursorUtil.getColumnIndexOrThrow(_cursor, "lectureId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfData = CursorUtil.getColumnIndexOrThrow(_cursor, "data");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfStrokeWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeWidth");
          final int _cursorIndexOfAlpha = CursorUtil.getColumnIndexOrThrow(_cursor, "alpha");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final NoteVisual _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLectureId;
            _tmpLectureId = _cursor.getString(_cursorIndexOfLectureId);
            final String _tmpPdfId;
            _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final VisualType _tmpType;
            _tmpType = __VisualType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final String _tmpData;
            _tmpData = _cursor.getString(_cursorIndexOfData);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final float _tmpStrokeWidth;
            _tmpStrokeWidth = _cursor.getFloat(_cursorIndexOfStrokeWidth);
            final float _tmpAlpha;
            _tmpAlpha = _cursor.getFloat(_cursorIndexOfAlpha);
            final String _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getString(_cursorIndexOfHlcTimestamp);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new NoteVisual(_tmpId,_tmpLectureId,_tmpPdfId,_tmpTimestamp,_tmpPageNumber,_tmpType,_tmpData,_tmpColor,_tmpStrokeWidth,_tmpAlpha,_tmpHlcTimestamp,_tmpIsDeleted,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NoteVisual>> observeUpdatesSince(final String since) {
    final String _sql = "SELECT * FROM note_visuals WHERE hlcTimestamp > ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"note_visuals"}, new Callable<List<NoteVisual>>() {
      @Override
      @NonNull
      public List<NoteVisual> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLectureId = CursorUtil.getColumnIndexOrThrow(_cursor, "lectureId");
          final int _cursorIndexOfPdfId = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfData = CursorUtil.getColumnIndexOrThrow(_cursor, "data");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfStrokeWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "strokeWidth");
          final int _cursorIndexOfAlpha = CursorUtil.getColumnIndexOrThrow(_cursor, "alpha");
          final int _cursorIndexOfHlcTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "hlcTimestamp");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<NoteVisual> _result = new ArrayList<NoteVisual>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteVisual _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLectureId;
            _tmpLectureId = _cursor.getString(_cursorIndexOfLectureId);
            final String _tmpPdfId;
            _tmpPdfId = _cursor.getString(_cursorIndexOfPdfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final VisualType _tmpType;
            _tmpType = __VisualType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final String _tmpData;
            _tmpData = _cursor.getString(_cursorIndexOfData);
            final int _tmpColor;
            _tmpColor = _cursor.getInt(_cursorIndexOfColor);
            final float _tmpStrokeWidth;
            _tmpStrokeWidth = _cursor.getFloat(_cursorIndexOfStrokeWidth);
            final float _tmpAlpha;
            _tmpAlpha = _cursor.getFloat(_cursorIndexOfAlpha);
            final String _tmpHlcTimestamp;
            _tmpHlcTimestamp = _cursor.getString(_cursorIndexOfHlcTimestamp);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new NoteVisual(_tmpId,_tmpLectureId,_tmpPdfId,_tmpTimestamp,_tmpPageNumber,_tmpType,_tmpData,_tmpColor,_tmpStrokeWidth,_tmpAlpha,_tmpHlcTimestamp,_tmpIsDeleted,_tmpUpdatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __VisualType_enumToString(@NonNull final VisualType _value) {
    switch (_value) {
      case DRAWING: return "DRAWING";
      case HIGHLIGHT: return "HIGHLIGHT";
      case BOX: return "BOX";
      case ERASER: return "ERASER";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private VisualType __VisualType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "DRAWING": return VisualType.DRAWING;
      case "HIGHLIGHT": return VisualType.HIGHLIGHT;
      case "BOX": return VisualType.BOX;
      case "ERASER": return VisualType.ERASER;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
