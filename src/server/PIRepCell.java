package server;

import javafx.scene.control.ListCell;
import javafx.scene.text.Text;

public class PIRepCell extends ListCell<LocalPIRepresentation> {
	
	public PIRepCell() {
		setMinHeight(80);
	}
	
	@Override
    public void updateItem(LocalPIRepresentation item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null) {
        	Text name = new Text(item.hostname);
            setGraphic(name);
        }
    }
	
}
