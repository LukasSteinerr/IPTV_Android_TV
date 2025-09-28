# Custom Compose EPG Implementation

This document describes the new custom Electronic Program Guide (EPG) implementation built with Jetpack Compose for TV, replacing the previous `android-tv-program-guide` plugin dependency.

## Overview

The new EPG implementation provides all the functionality of the original plugin but is built entirely in Compose for TV, making it:
- Fully customizable and owned by your project
- Better integrated with Compose UI
- More performant for TV interfaces
- Easier to maintain and extend

## Architecture

### Core Components

#### 1. Data Models (`epg/model/`)
- **`EpgData.kt`**: Main data container for EPG information
- **`EpgChannel`**: Represents a TV channel with logo and metadata
- **`EpgProgram`**: Represents a TV program with timing and content info
- **`EpgTimeSlot`**: Represents time intervals for the timeline

#### 2. UI Components (`epg/ui/components/`)
- **`EpgGrid.kt`**: Main grid layout showing channels and programs
- **`EpgChannel.kt`**: Individual channel item with logo and name
- **`EpgProgram.kt`**: Individual program item with title, time, and progress
- **`EpgTimeline.kt`**: Time ruler at the top with current time indicator
- **`EpgControls.kt`**: Date/time navigation controls and filters
- **`EpgProgramDetails.kt`**: Details panel showing selected program info

#### 3. Main Screen (`epg/ui/`)
- **`EpgScreen.kt`**: Main composable that coordinates all components
- **`EpgTheme.kt`**: Theme definitions and styling

## Key Features

### 1. TV-Optimized Navigation
- **D-pad Support**: Full navigation with TV remote controls
- **Focus Management**: Proper focus handling for TV interfaces
- **Keyboard Navigation**: Support for arrow key navigation

### 2. Time Navigation
- **Date Selection**: Choose from past and future dates
- **Time of Day Filters**: Jump to Morning/Afternoon/Evening
- **Jump to Live**: Quick navigation to current time for today
- **Timeline Scrolling**: Horizontal scrolling through program schedule

### 3. Program Information
- **Live Indicator**: Shows currently airing programs
- **Progress Bars**: Progress indication for live programs
- **Program Details**: Detailed view with description and timing
- **Channel Logos**: Display channel branding when available

### 4. Visual Design
- **TV-Optimized UI**: Large touch targets and clear focus indicators
- **Dark Theme**: TV-appropriate dark color scheme
- **Smooth Animations**: Fluid transitions and focus changes
- **Responsive Layout**: Adapts to different screen sizes

## Integration

The new EPG integrates seamlessly with your existing codebase:

### Usage
```kotlin
@Composable
fun MyTVScreen() {
    EpgScreen(
        playlist = myPlaylist,
        playlistService = playlistService,
        onChannelSelected = { channel ->
            // Handle channel selection
        },
        onBackPressed = {
            // Handle back navigation
        }
    )
}
```

### Data Loading
The EPG automatically loads data from your existing `PlaylistService`:
- Fetches channels from `getLiveTVChannelsForPlaylist()`
- Loads programs from `getEpgProgramsForChannel()`
- Handles errors gracefully with retry functionality

## Migration from android-tv-program-guide

### What Changed
1. **Removed Dependency**: No longer dependent on external EPG library
2. **Compose Native**: Built entirely with Compose for TV
3. **Simplified Integration**: Direct function calls instead of fragment management
4. **Better Performance**: Optimized for Compose rendering pipeline

### What Stayed the Same
1. **Data Sources**: Uses same `PlaylistService` and data models
2. **User Experience**: Same EPG functionality and navigation patterns
3. **Channel Selection**: Same callback mechanism for channel selection
4. **Program Display**: Same program information and timing display

### Migration Steps
1. ✅ Created new Compose EPG components
2. ✅ Updated `EpgComposable.kt` to use new implementation
3. ✅ Removed old EPG library dependency
4. ✅ Preserved all existing functionality

## Customization

### Theming
Modify `EpgTheme.kt` to customize:
- Colors (background, focus, current program indicators)
- Typography (font sizes, styles)
- Dimensions (channel width, program height, spacing)
- Shapes (corner radius, borders)

### Layout
Adjust component layouts in:
- `EpgGrid.kt`: Grid structure and scrolling behavior
- `EpgControls.kt`: Control panel layout and options
- `EpgProgramDetails.kt`: Details panel content and layout

### Behavior
Customize navigation and interaction in:
- `EpgScreen.kt`: Main screen coordination and state management
- Individual components: Focus handling and click behavior

## Technical Benefits

1. **No External Dependencies**: Complete control over EPG implementation
2. **Compose Integration**: Seamless integration with Compose UI patterns
3. **TV Optimization**: Built specifically for Android TV interfaces
4. **Maintainability**: Clear component structure and separation of concerns
5. **Performance**: Optimized rendering and memory usage
6. **Extensibility**: Easy to add new features and customizations

## Future Enhancements

The new architecture makes it easy to add:
- Multi-day view support
- Search and filtering capabilities
- Favorites and recommendations
- Recording schedule integration
- Parental controls
- Accessibility improvements

## Testing

The implementation includes:
- Component previews for UI testing
- Error state handling
- Loading state management
- Focus state testing
- Navigation flow validation
