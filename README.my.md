မြန်မာ | **[English](README.md)**

# ကိုးနဝင်း ပုတီးစိပ်နည်း

ကိုးနဝင်း ၈၁ ရက် ပုတီးစိပ်နည်း နေ့စဉ်မှတ်တမ်း အက်ပ် — နေ့တိုင်း ပုတီးစိပ်အရေအတွက်ကို မှတ်တမ်းတင်ပြီး ၈၁ ရက်ပြည့်အောင် ခြေရာခံနိုင်တဲ့ အက်ပ်။

## လုပ်ဆောင်ချက်များ

- **နေ့စဉ်မှတ်တမ်း** — နေ့တိုင်း စိပ်ပြီးတဲ့ ပုတီးအရေအတွက်ကို မှတ်တမ်းတင်နိုင်
- **၈၁ ရက် ခြေရာခံ** — ဘယ်နှရက်ရှိပြီ၊ ဘယ်နှရက်ကျန်သေးလဲ အလွယ်တကြည့်နိုင်
- **စက်တွင်းသိမ်းဆည်းမှု** — မှတ်တမ်းတွေဖုန်းထဲမှာပဲ သိမ်းထားပြီး အင်တာနက်မလိုဘဲ သုံးနိုင်
- **မြန်မာလို UI**

## Project structure

```
Koe-Na-Win/
├── android/        # Native Android project
├── app/            # App source
├── assets/         # Images, fonts
├── ios/            # iOS export scaffold
├── web/            # Web export scaffold
├── gradle/         # Gradle wrapper config
├── .env.example    # Required environment variables (copy to .env)
└── metadata.json   # Project metadata
```

## စတင်အသုံးပြုပုံ (Getting started)

1. `.env.example` ကို `.env` အဖြစ် ကူးပြီး လိုအပ်တဲ့ key တွေ ဖြည့်ပါ။
2. `android/` project ကို **Android Studio** မှာ ဖွင့်ပါ။
3. Gradle sync လုပ်ပြီး device/emulator ပေါ်မှာ run ပါ။

## Tech stack

- Kotlin, Android
- On-device local storage
- Google Gemini API (server-side) for smart features

## License

All rights reserved.
