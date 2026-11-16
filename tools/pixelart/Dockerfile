# Obraz roboczy do pixel-artu. Istnieje po to, zeby kazda iteracja nie placila
# 30 s za 'pip install pillow' — przy pracy graficznej liczy sie liczba petli
# "narysuj -> obejrzyj -> popraw" na godzine.
FROM python:3.12-slim
RUN pip install --no-cache-dir pillow==12.3.0 numpy
WORKDIR /w
