#include <jni.h>
#include "EventLog.h"
#include <windows.h>
#include <stdio.h>
#include <bits/stdc++.h>  
#include <winbase.h>
#include<string.h>
#include<vector>

#define BUFFER_SIZE 1024*128
#define MAX_TIMESTAMP_LEN       23 + 1 
#define MAX_WORD_LEN       1000 

using namespace std;


struct SearchRecord {
    string type;
    string time;
    string source;
    string eid;

};

void FillEventRecordDetails(std::vector<SearchRecord*> *searchRecordResult )
{

    HANDLE h;
    int i=0,j=0;
    EVENTLOGRECORD *pevlr;
    BYTE bBuffer[BUFFER_SIZE];
    DWORD dwRead, dwNeeded, dwThisRecord;

    // Open the Application event log.

    h = OpenEventLog( NULL,    
             "Application");   
    if (h == NULL)
    {
        printf("Could not open the Application event log.");
    }

    pevlr = (EVENTLOGRECORD *) &bBuffer;
    GetOldestEventLogRecord(h, &dwThisRecord);

    while (ReadEventLog(h,                // event log handle
                EVENTLOG_FORWARDS_READ |  // reads forward
                EVENTLOG_SEQUENTIAL_READ, // sequential read
                0,            // ignored for sequential reads
                pevlr,        // pointer to buffer
                BUFFER_SIZE,  // size of buffer
                &dwRead,      // number of bytes read
                &dwNeeded))   // bytes in next record
    {
        while (dwRead > 0)
        {


            //TIME
            string type;
            switch(pevlr->EventType)
            {
                case EVENTLOG_ERROR_TYPE:
                  // printf("ERROR\t  ");
                   type = "ERROR";
                    break;
                case EVENTLOG_WARNING_TYPE:
                   // printf("WARNING\t  ");
                    type = "WARNING";
                    break;
                case EVENTLOG_INFORMATION_TYPE:
                   // printf("INFORMATION \t ");
                    type = "INFORMATION";
                    break;
                case EVENTLOG_AUDIT_SUCCESS:
                 //  printf("AUDIT_SUCCESS\t  ");
                    type = "AUDIT_SUCCESS";
                    break;
                case EVENTLOG_AUDIT_FAILURE:
                 //  printf("AUDIT_FAILURE\t  ");
                    type = "AUDIT_FAILURE";
                    break;
                default:
                 //   printf("Unknown ");
                    type = "Unknown";
                    break;
            }

            //TIME
            DWORD Time = ((PEVENTLOGRECORD)pevlr)->TimeGenerated ;
            ULONGLONG ullTimeStamp = 0;
            ULONGLONG SecsTo1970 = 116444736000000000;
            SYSTEMTIME st;
            FILETIME ft, ftLocal;
            ullTimeStamp = Int32x32To64(Time, 10000000) + SecsTo1970;
            ft.dwHighDateTime = (DWORD)((ullTimeStamp >> 32) & 0xFFFFFFFF);
            ft.dwLowDateTime = (DWORD)(ullTimeStamp & 0xFFFFFFFF);   
            FileTimeToLocalFileTime(&ft, &ftLocal);
            FileTimeToSystemTime(&ftLocal, &st);   
            ostringstream mon1 , day1 ,year1,hour1,min1,sec1; 
            mon1 << st.wMonth ;day1 << st.wDay ;year1 << st.wYear ;hour1 << st.wHour ;min1 << st.wMinute ;sec1 << st.wSecond ;
            string mon = mon1.str();string day = day1.str();string year = year1.str();string hour = hour1.str();string min = min1.str();string sec = sec1.str();
            string time = day+"-"+mon+"-"+year+" "+hour+":"+min+":"+sec;

            int id = ((PEVENTLOGRECORD)pevlr)->EventID & 0xFFFF;
            ostringstream temp;
            temp << id;
            string eid = temp.str();  


            string source =  (LPSTR) ((LPBYTE) pevlr + sizeof(EVENTLOGRECORD));


            SearchRecord *pRecord = new SearchRecord();
            pRecord->type = type;
            pRecord->time = time;
            pRecord->eid = eid;
            pRecord->source = source;
            searchRecordResult->push_back(pRecord);

            dwRead -= pevlr->Length;
            pevlr = (EVENTLOGRECORD *)
                ((LPBYTE) pevlr + pevlr->Length);


        }

        pevlr = (EVENTLOGRECORD *) &bBuffer;
    }

    CloseEventLog(h);

}


extern "C"
JNIEXPORT jobjectArray JNICALL Java_EventLog_takeLogs
  (JNIEnv *env, jobject obj){

     vector<SearchRecord*> searchRecordResult ;
     FillEventRecordDetails(&searchRecordResult);
    // Get Properties class, its constructor and the put method
    jclass cls_Properties = env->FindClass("java/util/Properties");
    jmethodID mid_Properties_ctor = env->GetMethodID(cls_Properties, "<init>", "()V");
    jmethodID mid_Properties_put = env->GetMethodID(cls_Properties, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    // Construct the key Strings up front
    jstring key_type = env->NewStringUTF("type");
    jstring key_time = env->NewStringUTF("time");
    jstring key_source = env->NewStringUTF("source");
    jstring key_eid = env->NewStringUTF("eid");

    jobjectArray ret = env->NewObjectArray(searchRecordResult.size(), cls_Properties, 0);
    SearchRecord *result;
    for (int i = 0; i < searchRecordResult.size(); i++) {
    	  result=searchRecordResult[i];
        // Allocate and fill a Properties object, making sure to clean up the value Strings.
        env->PushLocalFrame(5);
        jobject prop = env->NewObject(cls_Properties, mid_Properties_ctor);
        env->CallObjectMethod(prop, mid_Properties_put, key_type, env->NewStringUTF(result->type.c_str()));
        env->CallObjectMethod(prop, mid_Properties_put, key_time, env->NewStringUTF(result->time.c_str()));
        env->CallObjectMethod(prop, mid_Properties_put, key_source, env->NewStringUTF(result->source.c_str()));
        env->CallObjectMethod(prop, mid_Properties_put, key_eid, env->NewStringUTF(result->eid.c_str()));
        prop = env->PopLocalFrame(prop);
        env->SetObjectArrayElement(ret, i, prop);
    }

    return ret;
}

