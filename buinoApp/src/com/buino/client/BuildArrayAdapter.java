package com.buino.client;

import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.buino.client.data.Build;
import com.buino.client.data.Build.BuildStatus;
import com.buino.client.util.ParseUtils;

/**
 * Creates our custom build list
 * 
 * @author takis
 * 
 */
public final class BuildArrayAdapter extends ArrayAdapter<Build> {

	static class ViewHolder {
		protected ImageView statusIcon;
		protected TextView name;
		protected TextView lastUpdate;
		protected CheckBox checkbox;
	}

	private final List<Build> allBuilds;
	private final Set<String> subscribedBuildNames;
	private final Activity context;

	public BuildArrayAdapter(final Activity context, final List<Build> list,
			final Set<String> subscribedBuildNames) {
		super(context, R.layout.build_list_row, list);
		this.context = context;
		this.allBuilds = list;
		this.subscribedBuildNames = subscribedBuildNames;
	}

	/**
	 * Returns all the builds that this list view has been initialized with
	 */
	public List<Build> getAllBuilds() {
		return allBuilds;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view = null;
		final Build currentElement = getAllBuilds().get(position);
		if (convertView == null) {
			final LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(R.layout.build_list_row, null);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.statusIcon = (ImageView) view.findViewById(R.id.statusIcon);
			viewHolder.name = (TextView) view.findViewById(R.id.name);
			viewHolder.lastUpdate = (TextView) view.findViewById(R.id.lastUpdate);
			viewHolder.checkbox = (CheckBox) view.findViewById(R.id.notifyCheckbox);
			view.setTag(viewHolder);
			viewHolder.checkbox.setTag(currentElement);
		} else {
			view = convertView;
			((ViewHolder) view.getTag()).checkbox.setTag(currentElement);
		}

		final ViewHolder holder = (ViewHolder) view.getTag();
		setStatusIcon(holder, currentElement);
		holder.name.setText(currentElement.getName());
		final String niceDate =
			DateFormat.format("EEEE, MMMM dd, h:mmaa", currentElement.getDateOfUpdate()).toString();
		holder.lastUpdate.setText("Updated: " + niceDate);
		setCheckBoxCheckedAndListener(holder, currentElement);
		return view;
	}

	/**
	 * Sets the checkbox that determines whether this build is subscribed to or not, and its listener
	 */
	private void setCheckBoxCheckedAndListener(final ViewHolder holder, final Build currentElement) {
		final String buildName = currentElement.getName();
		if (subscribedBuildNames.contains(buildName)) {
			holder.checkbox.setOnCheckedChangeListener(null);
			holder.checkbox.setChecked(true);
		} else {
			holder.checkbox.setOnCheckedChangeListener(null);
			holder.checkbox.setChecked(false);
		}
		holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
				if (isChecked) {
					ParseUtils.subscribeToBuildAndSync(buildName, context, holder.checkbox);
					Toast.makeText(context,
							"You will receive notifications for build \"" + buildName + "\"",
							Toast.LENGTH_SHORT).show();

				} else {
					ParseUtils.unsubscribeToBuildAndSync(buildName, context, holder.checkbox);
					Toast.makeText(context,
							"You will not receive notifications for build \"" + buildName + "\"",
							Toast.LENGTH_SHORT).show();

				}
			}
		});
	}

	/**
	 * Sets whether this element should have a grey/red/green lamp
	 */
	private void setStatusIcon(final ViewHolder holder, final Build currentElement) {
		final BuildStatus buildStatus = currentElement.getStatus();
		switch (buildStatus) {
		case FAILURE:
			holder.statusIcon.setImageResource(R.drawable.red_lamp);
			break;
		case SUCCESS:
			holder.statusIcon.setImageResource(R.drawable.green_lamp);
			break;
		default:
			holder.statusIcon.setImageResource(R.drawable.grey_lamp);
			break;
		}
	}

}
