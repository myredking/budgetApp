# n8n 공식 이미지를 기반으로 앨범 정규화용 FFmpeg를 추가합니다.
ARG N8N_IMAGE_TAG=latest
FROM n8nio/n8n:${N8N_IMAGE_TAG}

USER root

# n8n 공식 이미지에서 제거된 apk를 Alpine 베이스 이미지에서 복원합니다.
COPY --from=alpine:3.22 /sbin/apk /sbin/apk
COPY --from=alpine:3.22 /lib/apk /lib/apk
COPY --from=alpine:3.22 /usr/lib/libapk* /usr/lib/

RUN apk add --no-cache ffmpeg python3 py3-pip

COPY requirements.txt /workspace/requirements.txt
COPY budget /workspace/budget

RUN python3 -m pip install --break-system-packages --no-cache-dir \
    -r /workspace/requirements.txt

USER node
