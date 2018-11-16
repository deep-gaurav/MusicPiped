package deep.ryd.rydplayer;

import android.content.SearchRecentSuggestionsProvider;

public class mySuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "deep.ryd.rydplayer.SearchSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public mySuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
