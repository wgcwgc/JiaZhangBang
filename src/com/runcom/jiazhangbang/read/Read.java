/**
 * 
 */
package com.runcom.jiazhangbang.read;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;

import okhttp3.Call;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lamemp3.MP3Recorder;
import com.gr.okhttp.OkHttpUtils;
import com.gr.okhttp.callback.Callback;
import com.hxl.pauserecord.record.AudioRecorder;
import com.runcom.jiazhangbang.R;
import com.runcom.jiazhangbang.judge.Judge;
import com.runcom.jiazhangbang.listenText.GetLrcContents;
import com.runcom.jiazhangbang.listenText.MyAudio;
import com.runcom.jiazhangbang.storage.MySharedPreferences;
import com.runcom.jiazhangbang.util.NetUtil;
import com.runcom.jiazhangbang.util.PermissionUtil;
import com.runcom.jiazhangbang.util.URL;
import com.runcom.jiazhangbang.util.Util;
import com.umeng.analytics.MobclickAgent;

/**
 * @author Administrator
 * @copyright wgcwgc
 * @date 2017-4-12
 * @time 上午10:36:45
 * @project_name JiaZhangBang
 * @package_name com.runcom.jiazhangbang.repeat
 * @file_name Repeat.java
 * @type_name Repeat
 * @enclosing_type
 * @tags
 * @todo
 * @others
 * 
 */

@SuppressLint(
{ "FloatMath", "ClickableViewAccessibility" })
public class Read extends Activity
{
	private MP3Recorder mp3Recorder;
	private AudioRecorder audioRecorder;
	private String fileName = null;
	private Timer timer;
	private final String recordPath = Util.RECORDPATH_READ;
	private final ArrayList < String > myRecordList = new ArrayList < String >();// 待合成的录音片段
	private int second = 0;
	private int minute = 0;
	private int hour = 0;
	private TextView time;// 计时显示

	private Spinner spinner;
	private ImageButton startRecord , stopRecord;
	private final List < MyAudio > play_list = new ArrayList < MyAudio >();
	private final List < String > play_list_copy = new ArrayList < String >();
	private final List < String > play_list_id = new ArrayList < String >();
	private MyAudio myAudio;
	private int currIndex = 0;// 表示当前播放的音乐索引

	// 定义当前播放器的状态
	private static final int IDLE = 0;
	private static final int PAUSE = 1;
	private static final int START = 2;

	private int play_currentState = IDLE; // 当前播放器的状态
	private Intent intent;
	private String lyricsPath;
	private int course , grade , phase , unit;
	private ProgressDialog progressDialog;
	private TextView textView_lrcView;

	private int textSize = 0;
	@SuppressWarnings("unused")
	private final float oldDist = 0;
	@SuppressWarnings("unused")
	private final int mode = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.repeat_main);

		course = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.courseSharedPreferencesKeyString[0] ,0);
		course = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.courseSharedPreferencesKeyString[Util.Repeat] ,course) + 1;
		grade = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.gradeSharedPreferencesKeyString[0] ,0);
		grade = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.gradeSharedPreferencesKeyString[Util.Repeat] ,grade) + 1;
		phase = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.phaseSharedPreferencesKeyString[0] ,0);
		phase = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.phaseSharedPreferencesKeyString[Util.Repeat] ,phase) + 1;
		unit = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.unitSharedPreferencesKeyString[0] ,0);
		unit = MySharedPreferences.getValue(getApplicationContext() ,Util.settingChooseSharedPreferencesKey ,Util.unitSharedPreferencesKeyString[Util.Repeat] ,unit);

		ActionBar actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(false);
		actionbar.setDisplayShowHomeEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		actionbar.setDisplayShowTitleEnabled(true);
		actionbar.setDisplayShowCustomEnabled(true);
		String content = "朗读" + Util.grade[grade] + "上学期" + Util.unit[unit];
		if(2 == phase)
			content = "朗读" + Util.grade[grade] + "下学期" + Util.unit[unit];
		actionbar.setTitle(content);

		progressDialog = new ProgressDialog(this);
		progressDialog.setCancelable(false);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setMessage("正在获取数据......");
		progressDialog.show();

		initPlayView();
		new PermissionUtil(this , Manifest.permission.RECORD_AUDIO);
	}

	private void initPlayView()
	{
		spinner = (Spinner) findViewById(R.id.repeat_spinner);
		startRecord = (ImageButton) findViewById(R.id.media_start);
		stopRecord = (ImageButton) findViewById(R.id.media_stop);
		time = (TextView) findViewById(R.id.listen_write_textView_nameShow);
		textView_lrcView = (TextView) findViewById(R.id.listenText_lyricShow_textView);
		audioRecorder = AudioRecorder.getInstance(recordPath);
		initTitle();
	}

	/**
	 * 初始化数据
	 */
	private void initTitle()
	{
		final TreeMap < String , String > map = Util.getMap(getApplicationContext());
		map.put("uid" ,MySharedPreferences.getValue(getApplicationContext() ,Util.loginSharedPrefrencesKey ,"uid" ,null));
		map.put("course" ,course + "");
		map.put("grade" ,grade + "");
		map.put("phase" ,phase + "");
		map.put("unit" ,0 == unit ? -- unit + "" : unit + "");
		System.out.println(Util.REALSERVER + "gettextlist.php?" + URL.getParameter(map));
		OkHttpUtils.get().url(Util.REALSERVER + "gettextlist.php?" + URL.getParameter(map)).build().execute(new Callback < String >()
		{
			@Override
			public void onError(Call arg0 , Exception arg1 , int arg2 )
			{
				Toast.makeText(getApplicationContext() ,Util.okHttpUtilsConnectServerExceptionString ,Toast.LENGTH_LONG).show();
				finish();
			}

			@Override
			public void onResponse(String arg0 , int arg1 )
			{
				if(Util.okHttpUtilsResultOkStringValue.equalsIgnoreCase(arg0))
				{
					initData();
				}
				else
					if(Util.okHttpUtilsResultExceptionStringValue.equalsIgnoreCase(arg0))
					{
						Toast.makeText(getApplicationContext() ,Util.okHttpUtilsMissingResourceString ,Toast.LENGTH_LONG).show();
						finish();
					}
					else
					{
						Toast.makeText(getApplicationContext() ,Util.okHttpUtilsServerExceptionString ,Toast.LENGTH_LONG).show();
						finish();
					}
			}

			@Override
			public String parseNetworkResponse(Response arg0 , int arg1 ) throws Exception
			{
				String response = arg0.body().string().trim();
				JSONObject jsonObject = new JSONObject(response);
				String result = jsonObject.getString(Util.okHttpUtilsResultStringKey);
				if( !Util.okHttpUtilsResultOkStringValue.equalsIgnoreCase(result))
				{
					return result;
				}
				// play_list_title.clear();
				play_list_id.clear();
				// System.out.println(jsonObject.toString());
				JSONArray jsonArray = jsonObject.getJSONArray("textlist");
				JSONObject textListJsonObject = null;
				int leng = jsonArray.length();
				if(leng <= 0)
				{
					return Util.okHttpUtilsResultExceptionStringValue;
				}
				for(int i = 0 ; i < leng ; i ++ )
				{
					textListJsonObject = new JSONObject(jsonArray.getString(i));
					String parts = textListJsonObject.getString("parts");
					int part = Integer.valueOf(parts);
					if(1 == part)
					{
						// play_list_title.add(textListJsonObject.getString("title"));
						play_list_id.add(textListJsonObject.getString("id"));
					}
					else
						if(1 < part)
						{
							JSONArray subjsonArray = textListJsonObject.getJSONArray("partlist");
							int length = subjsonArray.length();
							for(int k = 0 ; k < length ; k ++ )
							{
								JSONObject subjsonObject = new JSONObject(subjsonArray.getString(k));
								// play_list_title.add(subjsonObject.getString("title"));
								play_list_id.add(subjsonObject.getString("id"));
							}
						}
						else
						{
							Toast.makeText(getApplicationContext() ,"服务器异常" ,Toast.LENGTH_LONG).show();
							finish();
						}
				}

				return result;
			}

		});

	}

	private void initData()
	{

		if(NetUtil.getNetworkState(getApplicationContext()) == NetUtil.NETWORK_NONE)
		{
			Toast.makeText(getApplicationContext() ,Util.okHttpUtilsInternetConnectExceptionString ,Toast.LENGTH_SHORT).show();
			startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
		}
		else
		{
			final String resourceServer = MySharedPreferences.getValue(getApplicationContext() ,Util.resourceUrlHeadSharedPreferencesKey ,Util.resourceUrlHeadSharedPreferencesKeyString ,Util.RESOURCESERVER);
			TreeMap < String , String > map = null;
			play_list.clear();
			// play_list_copy.clear();
			final int leng = play_list_id.size();
			for(int i = 0 ; i < leng ; i ++ )
			{
				final int ii = i;
				map = Util.getMap(getApplicationContext());
				map.put("uid" ,MySharedPreferences.getValue(getApplicationContext() ,Util.loginSharedPrefrencesKey ,"uid" ,null));
				map.put("textid" ,play_list_id.get(i));
				System.out.println(Util.REALSERVER + "getfulltext.php?" + URL.getParameter(map));
				OkHttpUtils.get().url(Util.REALSERVER + "getfulltext.php?" + URL.getParameter(map)).build().execute(new Callback < String >()
				{

					@Override
					public void onError(Call arg0 , Exception arg1 , int arg2 )
					{
						Toast.makeText(getApplicationContext() ,Util.okHttpUtilsServerExceptionString ,Toast.LENGTH_LONG).show();
						finish();
					}

					@Override
					public void onResponse(String arg0 , int arg1 )
					{
						if(leng - 1 == ii)
						{
							initSpinner();
						}
						else
							if( !Util.okHttpUtilsResultOkStringValue.equalsIgnoreCase(arg0))
							{
								Toast.makeText(getApplicationContext() ,Util.okHttpUtilsServerExceptionString ,Toast.LENGTH_LONG).show();
								finish();
							}
					}

					@Override
					public String parseNetworkResponse(Response arg0 , int arg1 ) throws Exception
					{
						String response = arg0.body().string().trim();
						JSONObject jsonObject = new JSONObject(response);
						String result = jsonObject.getString(Util.okHttpUtilsResultStringKey);
						if( !Util.okHttpUtilsResultOkStringValue.equalsIgnoreCase(result))
						{
							return result;
						}
						JSONObject jsonObject_attr = new JSONObject(jsonObject.getString("attr"));
						JSONObject jsonObject_partlist = new JSONObject(jsonObject_attr.getString("partlist"));

						myAudio = new MyAudio();
						String lyric_copy = resourceServer + jsonObject_partlist.getString("subtitle");
						String title = jsonObject_partlist.getString("title");
						// play_list_copy.add(title);
						myAudio.setName(title);
						// if( !new File(Util.LYRICSPATH + title +
						// ".lrc").exists())
						// new LrcFileDownloader(lyric_copy , title +
						// ".lrc").start();
						myAudio.setLyric(lyric_copy);
						String source_copy = resourceServer + jsonObject_partlist.getString("voice");
						myAudio.setSource(source_copy);
						play_list.add(myAudio);
						try
						{
							Thread.sleep(2 * 1000);
						}
						catch(InterruptedException e)
						{
							System.out.println(e);
						}
						return result;
					}

				});
			}
		}
	}

	private void initSpinner()
	{

		initLyric();
		play_list_copy.clear();
		for(int i = 0 ; i < play_list.size() ; i ++ )
		{
			play_list_copy.add(play_list.get(i).getName());
		}
		ArrayAdapter < String > adapter;
		adapter = new ArrayAdapter < String >(getApplicationContext() , R.layout.spinner_item , R.id.spinnerItem_textView , play_list_copy);

		spinner.setAdapter(adapter);
		progressDialog.dismiss();
		spinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView < ? > arg0 , View arg1 , int arg2 , long arg3 )
			{
				currIndex = arg2;
				initLyric();
			}

			@Override
			public void onNothingSelected(AdapterView < ? > arg0 )
			{
			}
		});

		stopRecord.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v )
			{
				if(play_currentState != IDLE)
				{
					stopRecord();
				}
				else
				{
					Toast.makeText(getApplicationContext() ,"请开始录音" ,Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@SuppressWarnings("unused")
	private float mLastMotionY;
	/**
	 * 第一个手指的坐标
	 **/
	private final PointF mPointerOneLastMotion = new PointF();
	/**
	 * 第二个手指的坐标
	 **/
	private final PointF mPointerTwoLastMotion = new PointF();
	/**
	 * 是否是第一次移动，当一个手指按下后开始移动的时候，设置为true, 当第二个手指按下的时候，即两个手指同时移动的时候，设置为false
	 */
	private boolean mIsFirstMove = false;

	private void initLyric()
	{
		if(play_list.size() <= 0)
		{
			Toast.makeText(Read.this ,"服务器异常" ,Toast.LENGTH_SHORT).show();
			return;
		}
		lyricsPath = play_list.get(currIndex).getLyric();
		String content = "";
		// content = Util.getLrcContents(lyricsPath);
		FutureTask < String > faeature = new FutureTask < String >(new GetLrcContents(lyricsPath));
		new Thread(faeature).start();
		try
		{
			content = faeature.get();
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		String contents[] = content.split("\n");
		content = "";
		for(int i = 0 , leng = contents.length ; i < leng ; i ++ )
		{
			content += (contents[i].substring(contents[i].indexOf("]") + 1) + "\n");
		}
		// File mFile = new File(lyricsPath);
		// if( !mFile.exists())
		// {
		// content = "\n\n\n暂无字幕";
		// }
		// else
		// {
		// FileInputStream mFileInputStream;
		// BufferedReader mBufferedReader = null;
		// String Lrc_data = "";
		// try
		// {
		// mFileInputStream = new FileInputStream(mFile);
		// InputStreamReader mInputStreamReader;
		// mInputStreamReader = new InputStreamReader(mFileInputStream ,
		// "utf-8");
		// mBufferedReader = new BufferedReader(mInputStreamReader);
		// while((Lrc_data = mBufferedReader.readLine()) != null)
		// {
		// content += (Lrc_data.substring(Lrc_data.indexOf("]") + 1) + "\n");
		// }
		//
		// }
		// catch(Exception e)
		// {
		// System.out.println(e);
		// }
		// finally
		// {
		// try
		// {
		// mBufferedReader.close();
		// }
		// catch(IOException e)
		// {
		// System.out.println(e);
		// }
		// }
		// }
		textView_lrcView.setText(content);
		textView_lrcView.setFocusable(true);
		// 缩放字体大小
		textView_lrcView.setOnTouchListener(new OnTouchListener()
		{

			@Override
			public boolean onTouch(View v , MotionEvent event )
			{
				if(textSize == 0)
				{
					textSize = (int) textView_lrcView.getTextSize();
				}

				switch(event.getAction() & MotionEvent.ACTION_MASK)
				{

				// 手指按下
					case MotionEvent.ACTION_DOWN:
						mLastMotionY = event.getY();
						mIsFirstMove = true;
						break;
					// 手指移动
					case MotionEvent.ACTION_MOVE:
						if(event.getPointerCount() == 2)
						{
							doScale(event);
							return true;
						}
						break;
					case MotionEvent.ACTION_CANCEL:
						// 手指抬起
					case MotionEvent.ACTION_UP:
						break;

				// case MotionEvent.ACTION_DOWN:
				// mode = 1;
				// break;
				//
				// case MotionEvent.ACTION_UP:
				// mode = 0;
				// break;
				//
				// case MotionEvent.ACTION_POINTER_DOWN:
				// mode += 1;
				// oldDist = spacing(event);
				// break;
				//
				// case MotionEvent.ACTION_POINTER_UP:
				// mode -= 1;
				// break;
				//
				// case MotionEvent.ACTION_MOVE:
				// if(mode == 2)
				// {
				// float newDist = spacing(event);
				//
				// if(newDist > oldDist + 1)
				// { // 放大
				// draw(newDist / oldDist);
				// oldDist = newDist;
				// }
				// if(newDist < oldDist - 1)
				// { // 缩小
				// draw(newDist / oldDist);
				// oldDist = newDist;
				// }
				// }
				// break;
				}

				return true;
			}
		});

	}

	/**
	 * 处理双指在屏幕移动时的，歌词大小缩放
	 */
	private void doScale(MotionEvent event )
	{
		if(mIsFirstMove)
		{
			mIsFirstMove = false;
			// 两个手指的x坐标和y坐标
			setTwoPointerLocation(event);
		}
		// 获取歌词大小要缩放的比例
		int scaleSize = getScale(event);
		// 如果缩放大小不等于0，进行缩放，重绘LrcView
		if(scaleSize != 0)
		{
			setNewFontSize(scaleSize);
		}
		setTwoPointerLocation(event);
	}

	/**
	 * 设置当前两个手指的x坐标和y坐标
	 */
	private void setTwoPointerLocation(MotionEvent event )
	{
		mPointerOneLastMotion.x = event.getX(0);
		mPointerOneLastMotion.y = event.getY(0);
		mPointerTwoLastMotion.x = event.getX(1);
		mPointerTwoLastMotion.y = event.getY(1);
	}

	/**
	 * 获取歌词大小要缩放的比例
	 */
	private int getScale(MotionEvent event )
	{
		float x0 = event.getX(0);
		float y0 = event.getY(0);
		float x1 = event.getX(1);
		float y1 = event.getY(1);

		float maxOffset = 0; // max offset between x or y axis,used to decide
		                     // scale size

		boolean zoomin = false;
		// 第一次双指之间的x坐标的差距
		float oldXOffset = Math.abs(mPointerOneLastMotion.x - mPointerTwoLastMotion.x);
		// 第二次双指之间的x坐标的差距
		float newXoffset = Math.abs(x1 - x0);

		// 第一次双指之间的y坐标的差距
		float oldYOffset = Math.abs(mPointerOneLastMotion.y - mPointerTwoLastMotion.y);
		// 第二次双指之间的y坐标的差距
		float newYoffset = Math.abs(y1 - y0);

		// 双指移动之后，判断双指之间移动的最大差距
		maxOffset = Math.max(Math.abs(newXoffset - oldXOffset) ,Math.abs(newYoffset - oldYOffset));
		// 如果x坐标移动的多一些
		if(maxOffset == Math.abs(newXoffset - oldXOffset))
		{
			// 如果第二次双指之间的x坐标的差距大于第一次双指之间的x坐标的差距则是放大，反之则缩小
			zoomin = newXoffset > oldXOffset ? true : false;
		}
		// 如果y坐标移动的多一些
		else
		{
			// 如果第二次双指之间的y坐标的差距大于第一次双指之间的y坐标的差距则是放大，反之则缩小
			zoomin = newYoffset > oldYOffset ? true : false;
		}
		if(zoomin)
		{
			return (int) (maxOffset / 10);// 放大双指之间移动的最大差距的1/10
		}
		else
		{
			return -(int) (maxOffset / 10);// 缩小双指之间移动的最大差距的1/10
		}
	}

	/**
	 * 设置缩放后的字体大小
	 */
	private void setNewFontSize(int scaleSize )
	{
		// 设置歌词缩放后的的最新字体大小
		textSize += scaleSize;
		textSize = Math.max(textSize ,12);
		textSize = Math.min(textSize ,57);
		textView_lrcView.setTextSize(textSize);

	}

	// 绘制TextView
	@SuppressWarnings("unused")
	private void draw(float f )
	{
		textSize *= f;
		if(textSize < 12)
		{
			textSize = 12;
		}

		if(textSize > 57)
		{
			textSize = 57;
		}

		textView_lrcView.setTextSize(textSize);

	}

	// 获取两指间的距离
	@SuppressWarnings("unused")
	private float spacing(MotionEvent event )
	{

		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);

		return FloatMath.sqrt(x * x + y * y);
	}

	public void onDetailSetting(View v )
	{
		intent = new Intent();
		intent.putExtra("selected" ,grade);
		intent.setClass(Read.this ,ReadList.class);
		startActivity(intent);
	}

	public void repeatSwitching(View v )
	{
		switch(play_currentState)
		{
			case IDLE:
				play_currentState = PAUSE;
				startRecord.setImageResource(R.drawable.play);
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						new Handler(Looper.getMainLooper()).post(new Runnable()
						{

							@Override
							public void run()
							{

								Toast.makeText(getApplicationContext() ,"3" ,Toast.LENGTH_SHORT).show();
								Toast.makeText(getApplicationContext() ,"2" ,Toast.LENGTH_SHORT).show();
								Toast.makeText(getApplicationContext() ,"1" ,Toast.LENGTH_SHORT).show();

							}
						});

						try
						{
							Thread.sleep(6 * 1000);
							startRecord();
							recordTime();
						}
						catch(InterruptedException e)
						{
							e.printStackTrace();
						}

					}
				}).start();

				break;
			case PAUSE:
				try
				{
					// audioRecorder.pauseRecord();
					mp3Recorder.pause();
					play_currentState = START;
					startRecord.setImageResource(R.drawable.record_pause);
				}
				catch(IllegalStateException e)
				{
				}
				if(timer != null)
				{
					timer.cancel();
				}

				break;
			case START:
				play_currentState = PAUSE;
				startRecord.setImageResource(R.drawable.play);
				// audioRecorder.startRecord(null);
				mp3Recorder.restore();
				recordTime();
				break;
			default:
				break;
		}

	}

	// 完成录音
	private void stopRecord()
	{
		if(timer != null)
		{
			timer.cancel();
		}

		try
		{
			// audioRecorder.stopRecord();
			mp3Recorder.stop();
			play_currentState = IDLE;
			startRecord.setImageResource(R.drawable.record_pause);

			final EditText editText = new EditText(Read.this);
			final AlertDialog.Builder inputDialog = new AlertDialog.Builder(Read.this);
			inputDialog.setTitle("要保存录音吗？").setView(editText);
			inputDialog.setPositiveButton("保存" ,new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog , int which )
				{
				}
			});

			inputDialog.setNegativeButton("放弃" ,new DialogInterface.OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog , int which )
				{
					time.setText("");
					// new File(recordPath + fileName + ".wav").delete();
					new File(recordPath + fileName + ".mp3").delete();
					File files[] = new File(recordPath).listFiles();
					for(File file : files)
					{
						if(file.toString().endsWith(".pcm"))
						{
							file.delete();
						}
					}

					for(int i = 0 ; i < myRecordList.size() ; i ++ )
					{
						File file = new File(myRecordList.get(i));
						if(file.exists())
						{
							file.delete();
						}
					}
				}
			});

			minute = 0;
			hour = 0;
			second = 0;

			final AlertDialog dialog = inputDialog.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view )
				{
					String playName = editText.getText().toString().trim();
					if(playName.isEmpty() || Judge.isNotName(playName))
					{
						Toast.makeText(getApplicationContext() ,"输入名字不符合要求，请重新命名" ,Toast.LENGTH_SHORT).show();
					}
					else
						if(isExit(playName))
						{
							Toast.makeText(getApplicationContext() ,"文件名重复，请重新命名" ,Toast.LENGTH_SHORT).show();
						}
						else
						{
							new File(recordPath + fileName + ".mp3").renameTo(new File(recordPath + playName + ".mp3"));
							Toast.makeText(getApplicationContext() ,playName + "录音完成" ,Toast.LENGTH_SHORT).show();
							time.setText("录音完成");
							for(int i = 0 ; i < myRecordList.size() ; i ++ )
							{
								File file = new File(myRecordList.get(i));
								if(file.exists())
								{
									file.delete();
								}
							}
							dialog.dismiss();
						}
				}
			});

		}
		catch(IllegalStateException e)
		{
		}
	}

	private Boolean isExit(String name )
	{
		File recordePathFile = new File(recordPath);
		if( !recordePathFile.exists())
		{
			try
			{
				recordePathFile.getParentFile().mkdirs();
				recordePathFile.mkdir();
			}
			catch(Exception e)
			{
			}
		}

		// 判断SD卡是否存在
		if( !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			Toast.makeText(this ,"SD卡状态异常，" ,Toast.LENGTH_LONG).show();
		}
		else
		{
			// 根据后缀名进行判断、获取文件夹中的音频文件
			File file = new File(recordPath);
			File files[] = file.listFiles();
			String childFileName = null;
			for(File childFile : files)
			{
				childFileName = childFile.toString();
				if(childFileName.length() > 0 && (childFileName.endsWith(".wav") || childFileName.endsWith(".mp3")))
				{
					if((childFileName.substring(childFileName.lastIndexOf("/") + 1 ,childFileName.lastIndexOf("."))).equals(name))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	// 开始录音
	private void startRecord()
	{
		File file = new File(recordPath);
		if( !file.exists())
		{
			file.mkdirs();
		}

		fileName = getTime();

		mp3Recorder = new MP3Recorder(recordPath , fileName);
		mp3Recorder.start();

		// audioRecorder.createDefaultAudio(fileName);
		// audioRecorder.startRecord(null);

	}

	// 计时器异步更新界面
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg )
		{
			time.setText("您本次的录音时长为：" + String.format("%1$02d:%2$02d:%3$02d" ,hour ,minute ,second));
			super.handleMessage(msg);
		}
	};

	// 录音计时
	private void recordTime()
	{
		TimerTask timerTask = new TimerTask()
		{

			@Override
			public void run()
			{
				second ++ ;
				if(second >= 60)
				{
					second = 0;
					minute ++ ;
					if(minute >= 60)
					{
						minute = 0;
						hour ++ ;
					}
				}
				handler.sendEmptyMessage(1);
			}

		};
		timer = new Timer();
		timer.schedule(timerTask ,1000 ,1000);
	}

	// 获得当前时间
	@SuppressLint("SimpleDateFormat")
	private String getTime()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		String time = formatter.format(curDate);
		return time;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu )
	{
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item )
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				audioRecorder.release();
				onBackPressed();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	// 重写按返回键退出播放
	@Override
	public boolean onKeyDown(int keyCode , KeyEvent event )
	{
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
		{
			audioRecorder.release();
			finish();
			return true;
		}
		return super.onKeyDown(keyCode ,event);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		MobclickAgent.onPause(this);
		if(audioRecorder.getStatus() == AudioRecorder.Status.STATUS_START)
		{
			audioRecorder.pauseRecord();
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(progressDialog != null)
		{
			progressDialog.dismiss();
		}
		audioRecorder.release();
	}

}
