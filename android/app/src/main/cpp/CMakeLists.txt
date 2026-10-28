cmake_minimum_required(VERSION 3.22.1)
project(ioquake3_android C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(IOQ3_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../ioq3)
set(EXTERNAL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../external)

# 1. SDL2
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
add_subdirectory(${EXTERNAL_DIR}/SDL2 ${CMAKE_CURRENT_BINARY_DIR}/SDL2_build EXCLUDE_FROM_ALL)

# 2. gl4es (built as static library to embed into ioquake3)
set(STATICLIB ON CACHE BOOL "" FORCE)
set(ANDROID ON CACHE BOOL "" FORCE)
set(NOX11 ON CACHE BOOL "" FORCE)
set(NOEGL ON CACHE BOOL "" FORCE)
set(USE_ANDROID_LOG ON CACHE BOOL "" FORCE)
set(DEFAULT_GLES "libGLESv2.so" CACHE STRING "" FORCE)
set(NO_INIT_CONSTRUCTOR ON CACHE BOOL "" FORCE)
add_definitions(-DGL4ES_COMPILE_FOR_USE_IN_SHARED_LIB)
add_subdirectory(${EXTERNAL_DIR}/gl4es ${CMAKE_CURRENT_BINARY_DIR}/gl4es_build EXCLUDE_FROM_ALL)

# 3. Thirdparty sources: JPEG & Ogg Vorbis
file(GLOB JPEG_SOURCES ${IOQ3_DIR}/code/thirdparty/jpeg-9f/j*.c)
file(GLOB OGG_SOURCES ${IOQ3_DIR}/code/thirdparty/libogg-1.3.6/src/*.c)
file(GLOB VORBIS_SOURCES ${IOQ3_DIR}/code/thirdparty/libvorbis-1.3.7/lib/*.c)

# 4. Core ioquake3 sources
set(IOQ3_SOURCES
    # qcommon
    ${IOQ3_DIR}/code/qcommon/cm_load.c
    ${IOQ3_DIR}/code/qcommon/cm_patch.c
    ${IOQ3_DIR}/code/qcommon/cm_polylib.c
    ${IOQ3_DIR}/code/qcommon/cm_test.c
    ${IOQ3_DIR}/code/qcommon/cm_trace.c
    ${IOQ3_DIR}/code/qcommon/cmd.c
    ${IOQ3_DIR}/code/qcommon/common.c
    ${IOQ3_DIR}/code/qcommon/cvar.c
    ${IOQ3_DIR}/code/qcommon/files.c
    ${IOQ3_DIR}/code/qcommon/md4.c
    ${IOQ3_DIR}/code/qcommon/md5.c
    ${IOQ3_DIR}/code/qcommon/msg.c
    ${IOQ3_DIR}/code/qcommon/net_chan.c
    ${IOQ3_DIR}/code/qcommon/net_ip.c
    ${IOQ3_DIR}/code/qcommon/huffman.c
    ${IOQ3_DIR}/code/qcommon/q_math.c
    ${IOQ3_DIR}/code/qcommon/q_shared.c
    ${IOQ3_DIR}/code/qcommon/unzip.c
    ${IOQ3_DIR}/code/qcommon/ioapi.c
    ${IOQ3_DIR}/code/qcommon/vm.c
    ${IOQ3_DIR}/code/qcommon/vm_interpreted.c
    ${IOQ3_DIR}/code/asm/ftola.c
    ${IOQ3_DIR}/code/asm/snapvector.c

    # server
    ${IOQ3_DIR}/code/server/sv_bot.c
    ${IOQ3_DIR}/code/server/sv_client.c
    ${IOQ3_DIR}/code/server/sv_ccmds.c
    ${IOQ3_DIR}/code/server/sv_game.c
    ${IOQ3_DIR}/code/server/sv_init.c
    ${IOQ3_DIR}/code/server/sv_main.c
    ${IOQ3_DIR}/code/server/sv_net_chan.c
    ${IOQ3_DIR}/code/server/sv_snapshot.c
    ${IOQ3_DIR}/code/server/sv_world.c

    # client
    ${IOQ3_DIR}/code/client/cl_cgame.c
    ${IOQ3_DIR}/code/client/cl_cin.c
    ${IOQ3_DIR}/code/client/cl_console.c
    ${IOQ3_DIR}/code/client/cl_input.c
    ${IOQ3_DIR}/code/client/cl_keys.c
    ${IOQ3_DIR}/code/client/cl_main.c
    ${IOQ3_DIR}/code/client/cl_net_chan.c
    ${IOQ3_DIR}/code/client/cl_parse.c
    ${IOQ3_DIR}/code/client/cl_scrn.c
    ${IOQ3_DIR}/code/client/cl_ui.c
    ${IOQ3_DIR}/code/client/cl_avi.c
    ${IOQ3_DIR}/code/client/snd_adpcm.c
    ${IOQ3_DIR}/code/client/snd_dma.c
    ${IOQ3_DIR}/code/client/snd_mem.c
    ${IOQ3_DIR}/code/client/snd_mix.c
    ${IOQ3_DIR}/code/client/snd_wavelet.c
    ${IOQ3_DIR}/code/client/snd_main.c
    ${IOQ3_DIR}/code/client/snd_codec.c
    ${IOQ3_DIR}/code/client/snd_codec_wav.c
    ${IOQ3_DIR}/code/client/snd_codec_ogg.c
    ${IOQ3_DIR}/code/client/snd_openal.c

    # botlib
    ${IOQ3_DIR}/code/botlib/be_aas_bspq3.c
    ${IOQ3_DIR}/code/botlib/be_aas_cluster.c
    ${IOQ3_DIR}/code/botlib/be_aas_debug.c
    ${IOQ3_DIR}/code/botlib/be_aas_entity.c
    ${IOQ3_DIR}/code/botlib/be_aas_file.c
    ${IOQ3_DIR}/code/botlib/be_aas_main.c
    ${IOQ3_DIR}/code/botlib/be_aas_move.c
    ${IOQ3_DIR}/code/botlib/be_aas_optimize.c
    ${IOQ3_DIR}/code/botlib/be_aas_reach.c
    ${IOQ3_DIR}/code/botlib/be_aas_route.c
    ${IOQ3_DIR}/code/botlib/be_aas_routealt.c
    ${IOQ3_DIR}/code/botlib/be_aas_sample.c
    ${IOQ3_DIR}/code/botlib/be_ai_char.c
    ${IOQ3_DIR}/code/botlib/be_ai_chat.c
    ${IOQ3_DIR}/code/botlib/be_ai_gen.c
    ${IOQ3_DIR}/code/botlib/be_ai_goal.c
    ${IOQ3_DIR}/code/botlib/be_ai_move.c
    ${IOQ3_DIR}/code/botlib/be_ai_weap.c
    ${IOQ3_DIR}/code/botlib/be_ai_weight.c
    ${IOQ3_DIR}/code/botlib/be_ea.c
    ${IOQ3_DIR}/code/botlib/be_interface.c
    ${IOQ3_DIR}/code/botlib/l_crc.c
    ${IOQ3_DIR}/code/botlib/l_libvar.c
    ${IOQ3_DIR}/code/botlib/l_log.c
    ${IOQ3_DIR}/code/botlib/l_memory.c
    ${IOQ3_DIR}/code/botlib/l_precomp.c
    ${IOQ3_DIR}/code/botlib/l_script.c
    ${IOQ3_DIR}/code/botlib/l_struct.c

    # sys
    ${IOQ3_DIR}/code/sys/con_log.c
    ${IOQ3_DIR}/code/sys/sys_main.c
    ${IOQ3_DIR}/code/sys/sys_unix.c
    con_android.c

    # sdl
    ${IOQ3_DIR}/code/sdl/sdl_glimp.c
    ${IOQ3_DIR}/code/sdl/sdl_input.c
    ${IOQ3_DIR}/code/sdl/sdl_snd.c
    ${IOQ3_DIR}/code/sdl/sdl_gamma.c

    # renderercommon
    ${IOQ3_DIR}/code/renderercommon/tr_font.c
    ${IOQ3_DIR}/code/renderercommon/tr_image_bmp.c
    ${IOQ3_DIR}/code/renderercommon/tr_image_jpg.c
    ${IOQ3_DIR}/code/renderercommon/tr_image_pcx.c
    ${IOQ3_DIR}/code/renderercommon/tr_image_png.c
    ${IOQ3_DIR}/code/renderercommon/tr_image_pvr.c
    ${IOQ3_DIR}/code/renderercommon/tr_image_tga.c
    ${IOQ3_DIR}/code/renderercommon/tr_noise.c
    ${IOQ3_DIR}/code/renderercommon/puff.c

    # renderergl1
    ${IOQ3_DIR}/code/renderergl1/tr_animation.c
    ${IOQ3_DIR}/code/renderergl1/tr_backend.c
    ${IOQ3_DIR}/code/renderergl1/tr_bsp.c
    ${IOQ3_DIR}/code/renderergl1/tr_cmds.c
    ${IOQ3_DIR}/code/renderergl1/tr_curve.c
    ${IOQ3_DIR}/code/renderergl1/tr_flares.c
    ${IOQ3_DIR}/code/renderergl1/tr_image.c
    ${IOQ3_DIR}/code/renderergl1/tr_init.c
    ${IOQ3_DIR}/code/renderergl1/tr_light.c
    ${IOQ3_DIR}/code/renderergl1/tr_main.c
    ${IOQ3_DIR}/code/renderergl1/tr_marks.c
    ${IOQ3_DIR}/code/renderergl1/tr_mesh.c
    ${IOQ3_DIR}/code/renderergl1/tr_model.c
    ${IOQ3_DIR}/code/renderergl1/tr_model_iqm.c
    ${IOQ3_DIR}/code/renderergl1/tr_scene.c
    ${IOQ3_DIR}/code/renderergl1/tr_shade.c
    ${IOQ3_DIR}/code/renderergl1/tr_shade_calc.c
    ${IOQ3_DIR}/code/renderergl1/tr_shader.c
    ${IOQ3_DIR}/code/renderergl1/tr_shadows.c
    ${IOQ3_DIR}/code/renderergl1/tr_sky.c
    ${IOQ3_DIR}/code/renderergl1/tr_surface.c
    ${IOQ3_DIR}/code/renderergl1/tr_world.c

    # thirdparty
    ${JPEG_SOURCES}
    ${OGG_SOURCES}
    ${VORBIS_SOURCES}
)

add_library(ioquake3 SHARED ${IOQ3_SOURCES})

target_include_directories(ioquake3 PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${IOQ3_DIR}/code
    ${IOQ3_DIR}/code/qcommon
    ${IOQ3_DIR}/code/asm
    ${IOQ3_DIR}/code/sys
    ${IOQ3_DIR}/code/renderercommon
    ${IOQ3_DIR}/code/renderergl1
    ${IOQ3_DIR}/code/thirdparty/jpeg-9f
    ${IOQ3_DIR}/code/thirdparty/libogg-1.3.6/include
    ${IOQ3_DIR}/code/thirdparty/libvorbis-1.3.7/include
    ${IOQ3_DIR}/code/thirdparty/libvorbis-1.3.7/lib
    ${EXTERNAL_DIR}/gl4es/include
    ${EXTERNAL_DIR}/SDL2/include
)

target_compile_definitions(ioquake3 PRIVATE
    BOTLIB
    USE_CODEC_VORBIS=1
    USE_INTERNAL_JPEG=1
    PRODUCT_VERSION="1.36_android"
    DEFAULT_BASEDIR="baseq3"
    __ANDROID__
)

target_compile_options(ioquake3 PRIVATE
    -Wno-deprecated-declarations
    -Wno-shift-negative-value
    -Wno-tautological-constant-out-of-range-compare
    -Wno-pointer-sign
)

target_link_libraries(ioquake3 PRIVATE
    SDL2
    GL
    android
    log
    GLESv2
    EGL
    z
    m
)
