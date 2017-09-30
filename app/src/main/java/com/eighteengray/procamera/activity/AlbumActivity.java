package com.eighteengray.procamera.activity;

import com.eighteengray.procamera.R;
import com.eighteengray.procamera.bean.ImageFolder;
import com.eighteengray.procamera.card.baserecycler.BaseDataBean;
import com.eighteengray.procamera.card.baserecycler.RecyclerLayout;
import com.eighteengray.procamera.component.DaggerAlbumComponent;
import com.eighteengray.procamera.contract.IAlbumContract;
import com.eighteengray.procamera.module.PresenterModule;
import com.eighteengray.procamera.presenter.AlbumPresenter;
import com.eighteengray.procamera.widget.dialogfragment.ImageFoldersDialogFragment;
import com.eighteengray.procameralibrary.common.Constants;
import com.eighteengray.procameralibrary.dataevent.ImageFolderEvent;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



/**
 * 修改方案：
 * AlbumActivity作为View，实现IView方法。
 * 通过Dagger注入一个Presenter，AlbumActivity执行Presenter的方法。
 * Presenter会先执行business中业务方法拿到数据，然后执行IView更新界面。business中业务方法返回Observerable。
 * business中业务方法，会调用model中持久层方法，无论是本地获取数据，还是retrofit/okhttp网络获取数据，全部都封装成Observerable返回。
 */
public class AlbumActivity extends BaseActivity implements IAlbumContract.IView
{
    @BindView(R.id.rl_pics_album)
    RecyclerLayout rl_pics_album;

    @BindView(R.id.tv_select_album)
    TextView tv_select_album;

    private ContentResolver mContentResolver;
    private int currentImageFolderNum = 1;
    private ArrayList<ImageFolder> imageFolderArrayList = new ArrayList<>();

    @Inject
    AlbumPresenter albumPresenter;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        ButterKnife.bind(this);
        mContentResolver = getContentResolver();
        EventBus.getDefault().register(this);
    }

    @Override
    protected int getLayoutResId()
    {
        return R.layout.activity_album;
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        DaggerAlbumComponent.builder().presenterModule(new PresenterModule(this)).build().inject(this);
        //获取数据
        rl_pics_album.showLoadingView();
        albumPresenter.getAlbumData(AlbumActivity.this);

        rl_pics_album.setRecyclerViewScroll(new RecyclerLayout.RecyclerViewScroll()
        {
            @Override
            public void refreshData()
            {
                albumPresenter.getAlbumData(AlbumActivity.this);
            }

            @Override
            public void getMoreData()
            {
                albumPresenter.getAlbumData(AlbumActivity.this);
            }

            @Override
            public void upScroll()
            {
                
            }

            @Override
            public void downScroll()
            {

            }
        });
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    @OnClick({R.id.tv_select_album})
    public void click(View view)
    {
        switch (view.getId())
        {
            case R.id.tv_select_album:
                ImageFoldersDialogFragment imageFoldersDialogFragment = new ImageFoldersDialogFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Constants.IMAGEFOLDERS, imageFolderArrayList);
                bundle.putInt(Constants.CURRENTFOLDERNUM, currentImageFolderNum);
                imageFoldersDialogFragment.setArguments(bundle);
                imageFoldersDialogFragment.show(getSupportFragmentManager(), "");
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageFolderSelected(ImageFolderEvent imageFolderEvent)
    {
        currentImageFolderNum = imageFolderEvent.getCurrentImageFolderNum();
        setAdapterData(imageFolderArrayList);
        tv_select_album.setText(imageFolderArrayList.get(currentImageFolderNum).getName());
    }


    @Override
    public void setAdapterData(List<ImageFolder> imageFolders)
    {
        imageFolderArrayList = (ArrayList<ImageFolder>) imageFolders;
        if(imageFolderArrayList != null && imageFolderArrayList.size() > 0)
        {
            List<String> imagePathList = imageFolderArrayList.get(currentImageFolderNum).getImagePathList();
            rl_pics_album.showRecyclerView(generateDataBeanList(imagePathList));
        }
    }

    private List<BaseDataBean<String>> generateDataBeanList(List<String> imageFolderArrayList){
        List<BaseDataBean<String>> list = new ArrayList<>();
        int size = imageFolderArrayList.size();
        for(int i = 0; i < size; i++){
            BaseDataBean<String> baseDataBean = new BaseDataBean<>(1, imageFolderArrayList.get(i));
            list.add(baseDataBean);
        }
        return list;
    }


}
