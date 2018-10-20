package deep.ryd.rydplayer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;

public class AddNewPlayList extends DialogFragment {
    Main2Activity activity;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.create_new_playlist)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.newplaylistname);
                        try{
                            String name = et.getText().toString();
                            Playlist.newPlaylist(name,activity);
                        }
                        catch (Exception e){

                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.addplaylistdialog,null));
        // Create the AlertDialog object and return it

        Dialog d =  builder.create();
        return d;
    }
    public Dialog importDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View b = activity.getLayoutInflater().inflate(R.layout.addplaylistdialog,null);
        EditText et = b.findViewById(R.id.newplaylistname);
        et.setHint("Playlist URL");
        builder.setMessage("Import Playlist")
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditText et = (EditText) ((AlertDialog) dialog).findViewById(R.id.newplaylistname);
                        try{
                            String name = et.getText().toString();
                            Playlist.importPlaylist(name,activity);

                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                })
                .setView(b);
        // Create the AlertDialog object and return it

        Dialog d =  builder.create();
        return d;
    }

}
