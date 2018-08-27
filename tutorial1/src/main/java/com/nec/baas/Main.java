package com.nec.baas;

import com.nec.baas.core.*;
import com.nec.baas.generic.NbGenericServiceBuilder;
import com.nec.baas.http.NbHttpClient;
import com.nec.baas.object.NbClause;
import com.nec.baas.object.NbObject;
import com.nec.baas.object.NbObjectBucket;
import com.nec.baas.object.NbQuery;

import java.util.List;

/**
 * Main
 */
public class Main {
    private static final boolean DEBUG = false;

    private static final String BUCKET_NAME = "TodoTutorial1";

    private NbService mService;

    public static void main(String[] args) {
        new Main().run(args);
    }

    private void run(String[] args) {
        if (args.length < 1) {
            usage();
            return;
        }

        setup();

        switch (args[0]) {
            case "list":
                getTodos();
                break;

            case "add":
                if (args.length < 2) {
                    usage();
                    return;
                }
                addTodo(args[1]);
                break;

            case "delete":
                if (args.length < 2) {
                    usage();
                    return;
                }
                deleteTodo(args[1]);
                break;

            default:
                usage();
                return;
        }


        try {
            Thread.sleep(10000);
            System.err.println("Timed out");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void usage() {
        System.out.println("Usage: tutorial1 <command>");
        System.out.println("command:");
        System.out.println("  list          - show all todo");
        System.out.println("  add <text>    - add todo");
        System.out.println("  delete <id>   - delete todo");
    }

    private void setup() {
        // 自己署名証明書を許可する場合は以下の行を有効にする。
        // Service セットアップより前に実施しなければならない。
        // この方法はセキュリティリスクがあるため、通常は正規の証明書を使用し、以下の行は無効にすること！
        NbHttpClient.getInstance().setAllowSelfSignedCertificate(true);

        // Service のセットアップ
        mService = new NbGenericServiceBuilder()
                .tenantId(Config.TENANT_ID).appId(Config.APP_ID).appKey(Config.APP_KEY)
                .endPointUri(Config.ENDPOINT_URI)
                .build();

        // 動作モード設定
        if (DEBUG) {
            NbSetting.setOperationMode(NbOperationMode.DEBUG);
        } else {
            NbSetting.setOperationMode(NbOperationMode.PRODUCTION);
        }
    }

    private NbObjectBucket getTodoBucket() {
        return mService.objectBucketManager().getBucket(BUCKET_NAME);
    }

    private void failure(int statusCode) {
        System.err.println("Error : statusCode = " + statusCode);
        System.exit(1);
    }

    /**
     * Todo 一覧の取得と表示
     */
    private void getTodos() {
        NbObjectBucket bucket = getTodoBucket();

        // クエリ設定
        NbClause clause = new NbClause();
        //clause.equals("key", "value");
        NbQuery query = new NbQuery()
                .setClause(clause)
                .setSortOrders("updatedAt");

        // クエリ実行
        bucket.query(query, new NbListCallback<NbObject>() {
            @Override
            public void onSuccess(List<NbObject> list) {
                onReceiveTodo(list);
                System.exit(0);
            }

            @Override
            public void onFailure(int statusCode, NbErrorInfo errorInfo) {
                failure(statusCode);
            }
        });
    }

    /**
     * Todo 受信表示
     * @param list
     */
    private void onReceiveTodo(List<NbObject> list) {
        for (NbObject obj : list) {
            String id = obj.getObjectId();
            String description = obj.getString("description", "???");
            System.out.println(id + " : " + description);
        }
    }

    /**
     * TODO 追加
     * @param text
     */
    private void addTodo(String text) {
        NbObject obj = new NbObject(mService, BUCKET_NAME);
        obj.put("description", text);

        obj.save(new NbCallback<NbObject>() {
            @Override
            public void onSuccess(NbObject object) {
                System.out.println("OK");
                System.exit(0);
            }

            @Override
            public void onFailure(int statusCode, NbErrorInfo errorInfo) {
                failure(statusCode);
            }
        });
    }

    /**
     * TODO削除
     * @param objectId
     */
    private void deleteTodo(String objectId) {
        NbObjectBucket bucket = getTodoBucket();

        bucket.getObject(objectId, new NbCallback<NbObject>() {
            @Override
            public void onSuccess(NbObject obj) {
                obj.deleteObject(new NbResultCallback() {
                    @Override
                    public void onSuccess() {
                        System.out.println("Deleted");
                        System.exit(0);
                    }

                    @Override
                    public void onFailure(int statusCode, NbErrorInfo errorInfo) {
                        failure(statusCode);
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, NbErrorInfo errorInfo) {
                failure(statusCode);
            }
        });
    }
}
