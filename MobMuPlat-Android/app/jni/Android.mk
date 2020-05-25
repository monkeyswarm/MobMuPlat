LOCAL_PATH := $(call my-dir)

#---------------------------------------------------------------

include $(CLEAR_VARS)
LOCAL_MODULE := pd
LOCAL_EXPORT_C_INCLUDES := ../../PdCore/src/main/jni/libpd/pure-data/src
LOCAL_SRC_FILES := ../../PdCore/src/main/libs/$(TARGET_ARCH_ABI)/libpd.so
ifneq ($(MAKECMDGOALS),clean)
    include $(PREBUILT_SHARED_LIBRARY)
endif

#---------------------------------------------------------------

include $(CLEAR_VARS)
LOCAL_MODULE := coll
LOCAL_C_INCLUDES := $(LOCAL_PATH)/jni
LOCAL_CFLAGS := -DPD
LOCAL_SRC_FILES := coll.c common/file.c
LOCAL_SHARED_LIBRARIES = pd
include $(BUILD_SHARED_LIBRARY)

#---------------------------------------------------------------
