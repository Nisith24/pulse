cat << 'INNER_EOF' > /tmp/LibraryScreen_patch.txt
<<<<<<< SEARCH
                    if (currentTab == LibraryTab.HOME) {
                        items(currentLectures, key = { it.id }) { lecture ->
                            LectureCard(
                                lecture = lecture,
                                isLibraryHome = currentTab == LibraryTab.HOME,
                                onLectureSelected = onLectureSelected,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onDelete = { viewModel.deleteLecture(it) },
                                onLongPress = { selectedLongPressLecture = it }
                            )
                        }
                    } else {
=======
                    if (currentTab == LibraryTab.HOME) {
                        if (recentLocalLectures.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("Continue Learning", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth())
                            }
                            items(recentLocalLectures, key = { "recent_${it.id}" }) { lecture ->
                                LectureCard(
                                    lecture = lecture, isLibraryHome = true,
                                    onLectureSelected = onLectureSelected, onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteLecture(it) }, onLongPress = { selectedLongPressLecture = it }
                                )
                            }
                        }

                        if (videoLocalLectures.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("My Videos", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth())
                            }
                            items(videoLocalLectures, key = { "video_${it.id}" }) { lecture ->
                                LectureCard(
                                    lecture = lecture, isLibraryHome = true,
                                    onLectureSelected = onLectureSelected, onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteLecture(it) }, onLongPress = { selectedLongPressLecture = it }
                                )
                            }
                        }

                        if (pdfLocalLectures.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("My PDFs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth())
                            }
                            items(pdfLocalLectures, key = { "pdf_${it.id}" }) { lecture ->
                                LectureCard(
                                    lecture = lecture, isLibraryHome = true,
                                    onLectureSelected = onLectureSelected, onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteLecture(it) }, onLongPress = { selectedLongPressLecture = it }
                                )
                            }
                        }
                    } else {
>>>>>>> REPLACE
INNER_EOF
python3 -c "
import sys
search = ''
replace = ''
with open('/tmp/LibraryScreen_patch.txt') as f:
    content = f.read()
    search = content.split('<<<<<<< SEARCH\n')[1].split('=======\n')[0]
    replace = content.split('=======\n')[1].split('>>>>>>> REPLACE\n')[0]
with open('app/src/main/java/com/pulse/presentation/library/LibraryScreen.kt') as f:
    text = f.read()
text = text.replace(search, replace)
with open('app/src/main/java/com/pulse/presentation/library/LibraryScreen.kt', 'w') as f:
    f.write(text)
"
