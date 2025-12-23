
const tracks = {
    'background-music': {
        src: '/static/assets/music/background-music.mp3',
        title: 'Расслабляющий саундтрек',
        duration: '2:30'
    },
    'track1': {
        src: '/static/assets/music/track1.mp3',
        title: 'Энергичная фоновая музыка',
        duration: '3:15'
    },
    'track2': {
        src: '/static/assets/music/track2.mp3',
        title: 'Космическая атмосфера',
        duration: '4:20'
    },
    'track3': {
        src: '/static/assets/music/track3.mp3',
        title: 'Песня для души',
        duration: '5:00'
    }
};

let currentTrack = 'background-music';

function playTrack(trackId) {
    const track = tracks[trackId];
    const player = document.getElementById('mainAudioPlayer');

    if (!track) {
        console.error('Трек не найден:', trackId);
        return;
    }

    player.src = track.src;

    document.querySelectorAll('.track-item').forEach(item => {
        item.classList.remove('active');
    });

    const trackItems = document.querySelectorAll('.track-item');
    for (let i = 0; i < trackItems.length; i++) {
        if (trackItems[i].textContent.includes(track.title.substring(0, 20))) {
            trackItems[i].classList.add('active');
            break;
        }
    }

    player.play().catch(e => {
        console.log('Автовоспроизведение заблокировано:', e);
        alert('Нажмите "Play" для запуска музыки');
    });

    currentTrack = trackId;

    console.log('Playing:', track.title);
}

function changeVolume(delta) {
    const player = document.getElementById('mainAudioPlayer');
    if (player) {
        player.volume = Math.max(0, Math.min(1, player.volume + delta));
    }
}

function nextTrack() {
    const trackIds = Object.keys(tracks);
    const currentIndex = trackIds.indexOf(currentTrack);
    const nextIndex = (currentIndex + 1) % trackIds.length;
    const nextTrackId = trackIds[nextIndex];

    playTrack(nextTrackId);
}

function prevTrack() {
    const trackIds = Object.keys(tracks);
    const currentIndex = trackIds.indexOf(currentTrack);
    const prevIndex = (currentIndex - 1 + trackIds.length) % trackIds.length;
    const prevTrackId = trackIds[prevIndex];

    playTrack(prevTrackId);
}

document.addEventListener('DOMContentLoaded', function() {
    const player = document.getElementById('mainAudioPlayer');
    if (player) {
        player.addEventListener('ended', function() {
            setTimeout(() => {
                nextTrack();
            }, 1000);
        });

        player.addEventListener('error', function(e) {
            console.error('Audio error:', e);
            alert('Ошибка загрузки аудиофайла. Проверьте путь к файлу.');
        });
    }
});